package com.microservice.reservas.service;

import com.microservice.reservas.dto.event.AvisoInternoEvent;
import com.microservice.reservas.dto.event.InvitacionEmpleadoEvent;
import com.microservice.reservas.dto.request.RecursoDTO;
import com.microservice.reservas.dto.request.HorarioSemanalDTO;
import com.microservice.reservas.dto.response.InvitacionResponseDTO;
import com.microservice.reservas.exception.InvitacionException;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.enums.EstadoAsociacion;
import com.microservice.reservas.enums.EstadoCuenta;
import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.repository.*;
import com.microservice.reservas.util.ValidacionUtil;
import com.microservice.reservas.util.JornadaUtil;
import com.microservice.reservas.security.PermisosRecurso;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecursoService {

    private final RecursoRepository recursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NegocioRepository negocioRepository;
    private final ServicioRepository servicioRepository;
    private final TurnoRepository turnoRepository;
    private final OutboxService outboxService;
    private final NegocioService negocioService;

    @Transactional
    public Recurso crearRecurso(RecursoDTO recursoDto, String emailLogueado) {
        Negocio negocio = negocioRepository.findByIdAndActivoTrue(recursoDto.getIdNegocio())
                .orElseThrow(() -> new RuntimeException("El negocio especificado no existe o está inactivo."));

        if (!negocio.getOwner().getEmail().equals(emailLogueado)) {
            throw new AccessDeniedException("Acceso denegado: Solo el dueño del negocio puede agregar recursos.");
        }

        ValidacionUtil.validarSemanaCompleta(recursoDto.getHorarios());
        validarHorarios(recursoDto.getHorarios(), negocio.getHorarios());

        Recurso recurso = new Recurso();
        recurso.setNombre(recursoDto.getNombre());
        recurso.setDescripcion(recursoDto.getDescripcion());
        recurso.setNegocio(negocio);
        recurso.setActivo(true);
        recurso.setHorarios(new ArrayList<>());

        if (recursoDto.getEmailUsuario() != null && !recursoDto.getEmailUsuario().isEmpty()) {
            Usuario usuario = usuarioRepository.findByEmailAndEstadoCuenta(recursoDto.getEmailUsuario(), EstadoCuenta.ACTIVO)
                    .orElseThrow(() -> new RuntimeException("No se encontró ningún usuario activo con el email " + recursoDto.getEmailUsuario()));

            recurso.setUsuario(usuario);
            emitirInvitacion(recurso);
        } else {
            recurso.setEstadoAsociacion(EstadoAsociacion.ACTIVO);
        }

        mapearHorarios(recurso, recursoDto.getHorarios());
        Recurso recursoGuardado = recursoRepository.save(recurso);

        if (recursoGuardado.getEstadoAsociacion() == EstadoAsociacion.PENDIENTE) {
            dispararEventoInvitacion(recursoGuardado);
        }

        return recursoGuardado;
    }

    @Transactional(readOnly = true)
    public InvitacionResponseDTO obtenerInvitacion(String token) {
        validarFormatoToken(token);
        Recurso recurso = recursoRepository.findByTokenInvitacion(token)
                .orElseThrow(this::invitacionInvalida);
        validarInvitacion(recurso);
        return new InvitacionResponseDTO(recurso.getNegocio().getNombre(), recurso.getNombre(),
                recurso.getFechaExpiracionInvitacion());
    }

    @Transactional
    public String aceptarInvitacion(String token) {
        Recurso recurso = obtenerInvitacionParaActualizar(token);

        recurso.setEstadoAsociacion(EstadoAsociacion.ACTIVO);
        recurso.setTokenInvitacion(null);
        recurso.setFechaExpiracionInvitacion(null);
        recursoRepository.save(recurso);

        outboxService.encolar("notificaciones.exchange", "avisos.internos.routing.key",
                new AvisoInternoEvent(recurso.getNegocio().getOwner().getEmail(), "Invitación Aceptada", recurso.getUsuario().getEmail() + " aceptó la invitación para " + recurso.getNombre()));

        return "¡Invitación aceptada exitosamente!";
    }

    @Transactional
    public String rechazarInvitacion(String token) {
        Recurso recurso = obtenerInvitacionParaActualizar(token);

        recurso.setEstadoAsociacion(EstadoAsociacion.RECHAZADO);
        recurso.setTokenInvitacion(null);
        recurso.setFechaExpiracionInvitacion(null);
        Usuario usuarioTemp = recurso.getUsuario();
        recurso.setUsuario(null);
        recursoRepository.save(recurso);

        outboxService.encolar("notificaciones.exchange", "avisos.internos.routing.key",
                new AvisoInternoEvent(recurso.getNegocio().getOwner().getEmail(), "Invitación Rechazada", (usuarioTemp != null ? usuarioTemp.getEmail() : "Un usuario") + " rechazó tu invitación para " + recurso.getNombre()));

        return "Invitación rechazada.";
    }

    @Transactional
    public Recurso actualizarRecurso(Long id, RecursoDTO recursoDto, String emailLogueado) {
        Recurso recurso = recursoRepository.findByIdActivoParaActualizar(id)
                .orElseThrow(() -> new RuntimeException("Recurso no encontrado o inactivo."));

        PermisosRecurso.validarGestion(recurso, emailLogueado);
        boolean esPropietario = PermisosRecurso.esPropietario(recurso, emailLogueado);
        if (!Objects.equals(recurso.getNegocio().getId(), recursoDto.getIdNegocio())) {
            throw new AccessDeniedException("Acceso denegado: No se puede cambiar el negocio del recurso.");
        }
        if (!esPropietario && (!Objects.equals(recurso.getNombre(), recursoDto.getNombre())
                || !Objects.equals(recurso.getUsuario().getEmail(), recursoDto.getEmailUsuario()))) {
            throw new AccessDeniedException("Acceso denegado: Solo el propietario puede cambiar el nombre o la asignación del recurso.");
        }
        ValidacionUtil.validarSemanaCompleta(recursoDto.getHorarios());
        validarHorarios(recursoDto.getHorarios(), recurso.getNegocio().getHorarios());

        if (esPropietario) {
            recurso.setNombre(recursoDto.getNombre());
        }
        recurso.setDescripcion(recursoDto.getDescripcion());
        mapearHorarios(recurso, recursoDto.getHorarios());

        if (!esPropietario) {
            return recursoRepository.save(recurso);
        }

        boolean dispararEvento = false;

        if (recursoDto.getEmailUsuario() != null && !recursoDto.getEmailUsuario().isEmpty()) {

            boolean requiereInvitacion = recurso.getUsuario() == null ||
                    !recurso.getUsuario().getEmail().equals(recursoDto.getEmailUsuario()) ||
                    recurso.getEstadoAsociacion() == EstadoAsociacion.RECHAZADO ||
                    (recurso.getEstadoAsociacion() == EstadoAsociacion.PENDIENTE && invitacionVencida(recurso));

            if (requiereInvitacion) {
                Usuario nuevoUsuario = usuarioRepository.findByEmailAndEstadoCuenta(recursoDto.getEmailUsuario(), EstadoCuenta.ACTIVO)
                        .orElseThrow(() -> new RuntimeException("No se encontró ningún usuario activo con ese email."));

                recurso.setUsuario(nuevoUsuario);
                emitirInvitacion(recurso);
                dispararEvento = true;
            }
        } else {
            recurso.setUsuario(null);
            recurso.setTokenInvitacion(null);
            recurso.setFechaExpiracionInvitacion(null);
            recurso.setEstadoAsociacion(EstadoAsociacion.ACTIVO);
        }

        Recurso recursoGuardado = recursoRepository.save(recurso);

        if (dispararEvento) {
            dispararEventoInvitacion(recursoGuardado);
        }

        return recursoGuardado;
    }

    public List<Recurso> obtenerTodos() {
        return recursoRepository.findByActivoTrue().stream()
                .filter(recurso -> Boolean.TRUE.equals(recurso.getNegocio().getActivo())).toList();
    }

    public Optional<Recurso> obtenerPorId(Long id, String emailLogueado) {
        return recursoRepository.findByIdAndActivoTrue(id).map(recurso -> {
            PermisosRecurso.validarGestion(recurso, emailLogueado);
            return recurso;
        });
    }

    public List<Recurso> obtenerPorNegocio(Long negocioId) {
        negocioRepository.findByIdAndActivoTrue(negocioId)
                .orElseThrow(() -> new RuntimeException("El negocio no existe o está inactivo."));
        return recursoRepository.findByNegocioIdAndActivoTrueOrderByNombreAsc(negocioId);
    }

    public List<Recurso> obtenerTodosPorNegocio(Long negocioId, String emailLogueado) {
        Negocio negocio = negocioService.obtenerPorId(negocioId, emailLogueado)
                .orElseThrow(() -> new RuntimeException("El negocio no existe o está inactivo."));
        if (negocio.getOwner().getEmail().equals(emailLogueado)) {
            return recursoRepository.findByNegocioIdOrderByNombreAsc(negocioId);
        }
        return recursoRepository.findByNegocioIdAndActivoTrueOrderByNombreAsc(negocioId).stream()
                .filter(recurso -> PermisosRecurso.esEmpleadoActivo(recurso, emailLogueado)).toList();
    }

    @Transactional
    public String eliminarRecurso(Long id, String emailLogueado) {
        Recurso recurso = recursoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El recurso no existe."));

        if (!recurso.getActivo()) {
            throw new RuntimeException("Ya estaba eliminado.");
        }

        if (!recurso.getNegocio().getOwner().getEmail().equals(emailLogueado)) {
            throw new AccessDeniedException("Acceso denegado: Solo el dueño del negocio puede eliminar recursos.");
        }

        recurso.setActivo(false);
        recursoRepository.save(recurso);

        List<Servicio> serviciosDelRecurso = servicioRepository.findByRecursoIdAndActivoTrue(id);
        for (Servicio servicio : serviciosDelRecurso) {
            servicio.setActivo(false);
            servicioRepository.save(servicio);
        }

        List<EstadoTurno> estadosACancelar = List.of(EstadoTurno.CONFIRMADO);
        List<Turno> turnosAfectados = turnoRepository.findByRecursoIdAndEstadoIn(id, estadosACancelar);

        for (Turno turno : turnosAfectados) {
            turno.setEstado(EstadoTurno.CANCELADO);
            turnoRepository.save(turno);
        }

        return "Recurso dado de baja exitosamente. Se eliminaron " + serviciosDelRecurso.size() +
                " servicios y se cancelaron " + turnosAfectados.size() + " turnos pendientes.";
    }

    public List<Recurso> obtenerMisAsignaciones(String emailLogueado) {
        return recursoRepository.findByUsuarioEmailAndActivoTrueOrderByNombreAsc(emailLogueado);
    }

    private void emitirInvitacion(Recurso recurso) {
        recurso.setEstadoAsociacion(EstadoAsociacion.PENDIENTE);
        recurso.setTokenInvitacion(UUID.randomUUID().toString());
        recurso.setFechaExpiracionInvitacion(LocalDateTime.now().plusDays(7));
    }

    private Recurso obtenerInvitacionParaActualizar(String token) {
        validarFormatoToken(token);
        Recurso recurso = recursoRepository.findByTokenInvitacionParaActualizar(token)
                .orElseThrow(this::invitacionInvalida);
        validarInvitacion(recurso);
        return recurso;
    }

    private void validarFormatoToken(String token) {
        if (token == null || token.isBlank() || token.length() > 100) {
            throw invitacionInvalida();
        }
    }

    private void validarInvitacion(Recurso recurso) {
        if (recurso.getEstadoAsociacion() != EstadoAsociacion.PENDIENTE
                || !Boolean.TRUE.equals(recurso.getActivo())
                || recurso.getNegocio() == null || !Boolean.TRUE.equals(recurso.getNegocio().getActivo())
                || recurso.getUsuario() == null || !recurso.getUsuario().isEnabled()) {
            throw invitacionInvalida();
        }
        // Las invitaciones anteriores sin fecha deben renovarse, nunca son perpetuas.
        if (invitacionVencida(recurso)) {
            throw new InvitacionException(HttpStatus.GONE,
                    "La invitación venció o es anterior al sistema de vencimiento. Pedile al propietario que renueve la asignación.");
        }
    }

    private boolean invitacionVencida(Recurso recurso) {
        return recurso.getFechaExpiracionInvitacion() == null
                || !recurso.getFechaExpiracionInvitacion().isAfter(LocalDateTime.now());
    }

    private InvitacionException invitacionInvalida() {
        return new InvitacionException(HttpStatus.NOT_FOUND,
                "La invitación no es válida, ya fue procesada o dejó de estar disponible.");
    }

    private void dispararEventoInvitacion(Recurso recurso) {
        InvitacionEmpleadoEvent evento = new InvitacionEmpleadoEvent(
                recurso.getUsuario().getEmail(),
                recurso.getNegocio().getNombre(),
                recurso.getNombre(),
                recurso.getTokenInvitacion()
        );
        outboxService.encolar("notificaciones.exchange", "invitacion.email.routing.key", evento);
    }

    private void mapearHorarios(Recurso recurso, List<HorarioSemanalDTO> horariosDto) {
        if (recurso.getHorarios() == null) {
            recurso.setHorarios(new ArrayList<>());
        }

        if (horariosDto != null) {
            for (HorarioSemanalDTO hDto : horariosDto) {
                HorarioRecurso existente = recurso.getHorarios().stream()
                        .filter(h -> h.getDiaSemana() == hDto.getDiaSemana())
                        .findFirst()
                        .orElse(null);

                if (existente != null) {
                    actualizarHorarioRecurso(existente, hDto);
                } else {
                    recurso.getHorarios().add(crearHorarioRecurso(hDto, recurso));
                }
            }
        }
    }

    private void actualizarHorarioRecurso(HorarioRecurso existente, HorarioSemanalDTO hDto) {
        existente.setActivo(hDto.getActivo());
        existente.setHoraInicio(hDto.getHoraInicio());
        existente.setHoraFin(hDto.getHoraFin());
        existente.setHoraInicioDescanso(hDto.getHoraInicioDescanso());
        existente.setHoraFinDescanso(hDto.getHoraFinDescanso());
    }

    private HorarioRecurso crearHorarioRecurso(HorarioSemanalDTO hDto, Recurso recurso) {
        HorarioRecurso nuevo = new HorarioRecurso();
        nuevo.setDiaSemana(hDto.getDiaSemana());
        nuevo.setRecurso(recurso);
        actualizarHorarioRecurso(nuevo, hDto);
        return nuevo;
    }

    private void validarHorarios(List<HorarioSemanalDTO> horariosRecurso, List<HorarioNegocio> horariosNegocio) {
        if (horariosRecurso == null || horariosRecurso.isEmpty()) {
            throw new RuntimeException("Debe enviar los horarios semanales del recurso.");
        }

        for (HorarioSemanalDTO hr : horariosRecurso) {
            if (hr.getActivo() == null || !hr.getActivo()) continue;
            // Fecha de referencia fija: el día semanal es siempre el inicio real de la jornada.
            LocalDate dia = LocalDate.of(2024, 1, 1).plusDays(hr.getDiaSemana().getValue() - 1L);
            var jornadaRecurso = JornadaUtil.jornada(dia, hr.getHoraInicio(), hr.getHoraFin());
            boolean dentroDelNegocio = false;
            for (LocalDate inicioJornada : List.of(dia.minusDays(1), dia)) {
                if (horariosNegocio == null) continue;
                for (HorarioNegocio hn : horariosNegocio) {
                    if (hn.getDiaSemana() != inicioJornada.getDayOfWeek() || !Boolean.TRUE.equals(hn.getAbierto())) continue;
                    JornadaUtil.bloques(inicioJornada, hn.getHoraApertura(), hn.getHoraCierre(),
                            hn.getHoraInicioDescanso(), hn.getHoraFinDescanso());
                    var jornadaNegocio = JornadaUtil.jornada(inicioJornada, hn.getHoraApertura(), hn.getHoraCierre());
                    if (jornadaNegocio.contiene(jornadaRecurso.inicio(), jornadaRecurso.fin())) dentroDelNegocio = true;
                }
            }
            if (!dentroDelNegocio) {
                throw new RuntimeException("La jornada del recurso el " + hr.getDiaSemana() + " debe estar dentro del horario del negocio.");
            }
        }
    }
}
