package com.microservice.reservas.service;

import com.microservice.reservas.dto.request.RecursoDTO;
import com.microservice.reservas.dto.request.HorarioSemanalDTO;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.enums.EstadoCuenta;
import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecursoService {

    private final RecursoRepository recursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NegocioRepository negocioRepository;
    private final ServicioRepository servicioRepository;
    private final TurnoRepository turnoRepository;

    public Recurso crearRecurso(RecursoDTO recursoDto, String emailLogueado) {
        Negocio negocio = negocioRepository.findByIdAndActivoTrue(recursoDto.getIdNegocio())
                .orElseThrow(() -> new RuntimeException("El negocio especificado no existe o está inactivo."));

        if (!negocio.getOwner().getEmail().equals(emailLogueado)) {
            throw new RuntimeException("Acceso denegado: Solo el dueño del negocio puede agregar recursos.");
        }

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
        }

        mapearHorarios(recurso, recursoDto.getHorarios());

        return recursoRepository.save(recurso);
    }

    public Recurso actualizarRecurso(Long id, RecursoDTO recursoDto, String emailLogueado) {
        Recurso recurso = recursoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RuntimeException("Recurso no encontrado o inactivo."));

        // CANDADO DUAL: ¿Es el dueño o es el empleado asignado?
        validarPermisos(recurso, emailLogueado);

        validarHorarios(recursoDto.getHorarios(), recurso.getNegocio().getHorarios());

        recurso.setNombre(recursoDto.getNombre());
        recurso.setDescripcion(recursoDto.getDescripcion());

        mapearHorarios(recurso, recursoDto.getHorarios());

        return recursoRepository.save(recurso);
    }

    public List<Recurso> obtenerTodos() {
        return recursoRepository.findByActivoTrue();
    }

    public Optional<Recurso> obtenerPorId(Long id) {
        return recursoRepository.findByIdAndActivoTrue(id);
    }

    public List<Recurso> obtenerPorNegocio(Long negocioId) {
        return recursoRepository.findByNegocioIdAndActivoTrue(negocioId);
    }

    public List<Recurso> obtenerTodosPorNegocio(Long negocioId) {
        return recursoRepository.findByNegocioId(negocioId);
    }

    public String eliminarRecurso(Long id, String emailLogueado) {
        Recurso recurso = recursoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El recurso no existe."));

        if (!recurso.getActivo()) {
            throw new RuntimeException("Ya estaba eliminado.");
        }

        // SEGURIDAD ESTRICTA: Solo el dueño puede eliminar al recurso. El empleado no puede auto-eliminarse.
        if (!recurso.getNegocio().getOwner().getEmail().equals(emailLogueado)) {
            throw new RuntimeException("Acceso denegado: Solo el dueño del negocio puede eliminar recursos.");
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
        return recursoRepository.findByUsuarioEmailAndActivoTrue(emailLogueado);
    }

    // --- MÉTODOS PRIVADOS ---
    private void validarPermisos(Recurso recurso, String emailLogueado) {
        String emailOwnerNegocio = recurso.getNegocio().getOwner().getEmail();
        boolean esElOwner = emailOwnerNegocio.equals(emailLogueado);
        boolean esElRecurso = recurso.getUsuario() != null && recurso.getUsuario().getEmail().equals(emailLogueado);

        if (!esElOwner && !esElRecurso) {
            throw new RuntimeException("Acceso denegado: Solo el dueño del negocio o el recurso asignado pueden modificar la configuración.");
        }
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
        nuevo.setActivo(hDto.getActivo());
        nuevo.setHoraInicio(hDto.getHoraInicio());
        nuevo.setHoraFin(hDto.getHoraFin());
        nuevo.setHoraInicioDescanso(hDto.getHoraInicioDescanso());
        nuevo.setHoraFinDescanso(hDto.getHoraFinDescanso());
        nuevo.setRecurso(recurso);
        return nuevo;
    }

    private void validarHorarios(List<HorarioSemanalDTO> horariosRecurso, List<HorarioNegocio> horariosNegocio) {
        if (horariosRecurso == null || horariosRecurso.isEmpty()) {
            throw new RuntimeException("Debe enviar los horarios semanales del recurso.");
        }

        for (HorarioSemanalDTO hr : horariosRecurso) {
            if (hr.getActivo() == null || !hr.getActivo()) continue;

            HorarioNegocio hn = horariosNegocio.stream()
                    .filter(h -> h.getDiaSemana() == hr.getDiaSemana())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Falta configuración del negocio para el día " + hr.getDiaSemana()));

            if (!hn.getAbierto()) {
                throw new RuntimeException("El recurso no puede trabajar el " + hr.getDiaSemana() + " porque el negocio está cerrado.");
            }

            if (hr.getHoraInicio() == null || hr.getHoraFin() == null) {
                throw new RuntimeException("Debe especificar hora de inicio y fin para los días activos del recurso.");
            }

            if (hn.getHoraApertura() == null || hn.getHoraCierre() == null) {
                throw new RuntimeException("El negocio tiene horarios incompletos en el día " + hr.getDiaSemana());
            }

            if (hr.getHoraInicio().isBefore(hn.getHoraApertura()) || hr.getHoraFin().isAfter(hn.getHoraCierre())) {
                throw new RuntimeException("El horario del recurso el día " + hr.getDiaSemana() + " excede el horario de apertura del negocio (" + hn.getHoraApertura() + " a " + hn.getHoraCierre() + ").");
            }
        }
    }
}