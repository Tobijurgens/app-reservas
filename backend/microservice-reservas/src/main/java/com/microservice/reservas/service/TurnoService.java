package com.microservice.reservas.service;

import com.microservice.reservas.dto.event.AvisoInternoEvent;
import com.microservice.reservas.dto.event.TurnoEvent;
import com.microservice.reservas.dto.request.TurnoDTO;
import com.microservice.reservas.dto.response.HorarioDisponibleDTO;
import com.microservice.reservas.dto.response.ReporteResponseDTO;
import com.microservice.reservas.enums.EstadoCuenta;
import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.repository.*;
import com.microservice.reservas.security.PermisosRecurso;
import com.microservice.reservas.exception.TurnoException;
import com.microservice.reservas.util.JornadaUtil;
import com.microservice.reservas.util.JornadaUtil.Intervalo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private final TurnoRepository turnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RecursoRepository recursoRepository;
    private final ServicioRepository servicioRepository;
    private final NegocioRepository negocioRepository;
    private final OutboxService outboxService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Turno crearTurno(TurnoDTO turnoDto, String emailCliente) {
        if (turnoDto == null || turnoDto.getFechaHoraInicio() == null || turnoDto.getIdServicio() == null
                || turnoDto.getIdServicio() <= 0 || (turnoDto.getIdRecurso() != null && turnoDto.getIdRecurso() <= 0)) {
            throw new TurnoException(HttpStatus.BAD_REQUEST, "Indicá un servicio, un recurso válido y una fecha de inicio.");
        }
        if (!turnoDto.getFechaHoraInicio().isAfter(LocalDateTime.now())) {
            throw new TurnoException(HttpStatus.BAD_REQUEST, "La fecha del turno debe ser a futuro.");
        }
        if (emailCliente == null || emailCliente.isBlank()) {
            throw new TurnoException(HttpStatus.UNAUTHORIZED, "Iniciá sesión para reservar.");
        }

        Usuario cliente = usuarioRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new TurnoException(HttpStatus.UNAUTHORIZED, "El cliente autenticado no existe."));

        if (cliente.getEstadoCuenta() != EstadoCuenta.ACTIVO) {
            throw new AccessDeniedException("La cuenta del cliente no está habilitada para reservar.");
        }

        Servicio servicio = servicioRepository.findByIdAndActivoTrue(turnoDto.getIdServicio())
                .orElseThrow(() -> new TurnoException(HttpStatus.NOT_FOUND, "El servicio no existe o está inactivo."));

        if (!Boolean.TRUE.equals(servicio.getActivo()) || servicio.getRecurso() == null) {
            throw new TurnoException(HttpStatus.NOT_FOUND, "El servicio no está disponible.");
        }
        Long recursoId = turnoDto.getIdRecurso() != null ? turnoDto.getIdRecurso() : servicio.getRecurso().getId();
        // Todas las creaciones serializan ocupaciones sobre esta misma fila hasta el commit.
        Recurso recurso = recursoRepository.findByIdActivoParaActualizar(recursoId)
                .orElseThrow(() -> new TurnoException(HttpStatus.NOT_FOUND, "El recurso no existe o está inactivo."));

        validarRecursoYServicio(recurso, servicio);

        LocalDateTime fechaInicio = turnoDto.getFechaHoraInicio();
        // El inicio puede haber quedado en el pasado mientras esperaba el bloqueo.
        if (!fechaInicio.isAfter(LocalDateTime.now())) {
            throw new TurnoException(HttpStatus.BAD_REQUEST, "La fecha del turno debe ser a futuro.");
        }
        LocalDateTime fechaFin = fechaInicio.plusMinutes(servicio.getDuracionMinutos());
        validarHorarioReserva(recurso, fechaInicio, fechaFin);

        boolean ocupado = turnoRepository.existeSolapamiento(recurso.getId(), fechaInicio, fechaFin, EstadoTurno.CONFIRMADO);
        if (ocupado) {
            throw new TurnoException(HttpStatus.CONFLICT, "El horario seleccionado ya no está disponible. Elegí otro horario.");
        }

        Turno turno = new Turno();
        turno.setFechaHoraInicio(fechaInicio);
        turno.setFechaHoraFin(fechaFin);
        turno.setEstado(EstadoTurno.CONFIRMADO);
        turno.setCliente(cliente);
        turno.setRecurso(recurso);
        turno.setServicio(servicio);

        Turno turnoGuardado = turnoRepository.save(turno);

        dispararEventoTurno(turnoGuardado, "CONFIRMACION");

        String nombreCliente = cliente.getNombre() + " " + cliente.getApellido();
        String nombreNegocio = recurso.getNegocio().getNombre();

        outboxService.encolar("notificaciones.exchange", "avisos.internos.routing.key",
                new AvisoInternoEvent(recurso.getNegocio().getOwner().getEmail(), "Nuevo Turno", nombreCliente + " reservó en " + nombreNegocio + "con " + recurso.getNombre()));

        if (recurso.getUsuario() != null && !recurso.getUsuario().getEmail().equals(recurso.getNegocio().getOwner().getEmail())) {
            outboxService.encolar("notificaciones.exchange", "avisos.internos.routing.key",
                    new AvisoInternoEvent(recurso.getUsuario().getEmail(), "Nuevo Turno Asignado", nombreCliente + " reservó con vos en " + nombreNegocio));
        }
        return turnoGuardado;
    }

    @Transactional
    public Turno actualizarEstado(Long id, EstadoTurno nuevoEstado, String emailLogueado) {
        Turno turno = turnoRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new TurnoException(HttpStatus.NOT_FOUND, "El turno no existe."));

        validarPermisosDeModificacion(turno, emailLogueado);
        if (nuevoEstado == null) {
            throw new TurnoException(HttpStatus.BAD_REQUEST, "Indicá el nuevo estado.");
        }
        if (nuevoEstado == EstadoTurno.CONFIRMADO) {
            if (turno.getEstado() != EstadoTurno.CONFIRMADO) {
                throw new TurnoException(HttpStatus.CONFLICT, "No se puede reconfirmar un turno cancelado. Creá una nueva reserva.");
            }
            return turno; // Repetir CONFIRMADO no ocupa otro intervalo ni vuelve a guardar.
        }
        return cancelarTurnoValidado(turno);
    }

    public Page<Turno> obtenerTurnosPorNegocio(Long negocioId, Pageable pageable, String emailLogueado) {
        Negocio negocio = negocioRepository.findByIdAndActivoTrue(negocioId)
                .orElseThrow(() -> new RuntimeException("El negocio no existe."));

        if (!negocio.getOwner().getEmail().equals(emailLogueado)) {
            throw new AccessDeniedException("Acceso denegado: Solo el dueño puede ver todos los turnos del negocio.");
        }
        return turnoRepository.findByNegocioIdDesc(negocioId, pageable);
    }

    public Page<Turno> obtenerHistorialPorRecurso(Long recursoId, Pageable pageable, String emailLogueado) {
        Recurso recurso = recursoRepository.findByIdAndActivoTrue(recursoId)
                .orElseThrow(() -> new RuntimeException("El recurso no existe."));

        PermisosRecurso.validarGestion(recurso, emailLogueado);
        return turnoRepository.findByRecursoIdDesc(recursoId, pageable);
    }

    public Optional<Turno> obtenerPorId(Long id, String emailLogueado) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El turno no existe."));

        validarPermisosDeModificacion(turno, emailLogueado);
        return Optional.of(turno);
    }

    public List<Turno> obtenerMisTurnos(String emailCliente) {
        Usuario cliente = usuarioRepository.findByEmailAndEstadoCuenta(emailCliente, EstadoCuenta.ACTIVO)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado o inactivo."));

        return turnoRepository.findByClienteId(cliente.getId());
    }

    public List<Turno> obtenerAgendaRecurso(Long recursoId, String emailLogueado) {
        Recurso recurso = recursoRepository.findByIdAndActivoTrue(recursoId)
                .orElseThrow(() -> new RuntimeException("El recurso no existe."));

        PermisosRecurso.validarGestion(recurso, emailLogueado);
        return turnoRepository.findByRecursoId(recursoId);
    }

    public List<Turno> obtenerTurnosOcupados(Long recursoId, LocalDateTime inicioDia, LocalDateTime finDia, String emailLogueado) {
        Recurso recurso = recursoRepository.findByIdAndActivoTrue(recursoId)
                .orElseThrow(() -> new RuntimeException("El recurso no existe."));

        PermisosRecurso.validarGestion(recurso, emailLogueado);
        return turnoRepository.findTurnosOcupados(recursoId, inicioDia, finDia, EstadoTurno.CONFIRMADO);
    }

    @Transactional
    public String cancelarTurno(Long id, String emailLogueado) {
        Turno turno = turnoRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new TurnoException(HttpStatus.NOT_FOUND, "El turno no existe."));

        validarPermisosDeModificacion(turno, emailLogueado);
        cancelarTurnoValidado(turno);
        return "Turno cancelado correctamente.";
    }

    private Turno cancelarTurnoValidado(Turno turno) {
        if (turno.getEstado() == EstadoTurno.CANCELADO) {
            throw new TurnoException(HttpStatus.CONFLICT, "El turno ya se encontraba cancelado.");
        }

        turno.setEstado(EstadoTurno.CANCELADO);
        Turno turnoCancelado = turnoRepository.save(turno);

        dispararEventoTurno(turnoCancelado, "CANCELACION");

        String nombreCliente = turno.getCliente().getNombre() + " " + turno.getCliente().getApellido();

        // Aviso al dueño
        outboxService.encolar("notificaciones.exchange", "avisos.internos.routing.key",
                new AvisoInternoEvent(turno.getRecurso().getNegocio().getOwner().getEmail(), "Turno Cancelado", nombreCliente + " canceló su turno."));

        // Aviso al recurso
        if (turno.getRecurso().getUsuario() != null && !turno.getRecurso().getUsuario().getEmail().equals(turno.getRecurso().getNegocio().getOwner().getEmail())) {
            outboxService.encolar("notificaciones.exchange", "avisos.internos.routing.key",
                    new AvisoInternoEvent(turno.getRecurso().getUsuario().getEmail(), "Turno Cancelado", nombreCliente + " canceló su turno con vos."));
        }

        return turnoCancelado;
    }

    @Transactional(readOnly = true)
    public List<HorarioDisponibleDTO> obtenerHorariosDisponibles(LocalDate fecha, Long recursoId, Long servicioId) {
        if (fecha.isBefore(LocalDate.now())) return new ArrayList<>();

        Recurso recurso = recursoRepository.findByIdAndActivoTrue(recursoId)
                .orElseThrow(() -> new TurnoException(HttpStatus.NOT_FOUND, "El recurso no existe o está inactivo."));
        Servicio servicio = servicioRepository.findByIdAndActivoTrue(servicioId)
                .orElseThrow(() -> new TurnoException(HttpStatus.NOT_FOUND, "El servicio no existe o está inactivo."));
        validarRecursoYServicio(recurso, servicio);

        List<Intervalo> bloques = bloquesReserva(recurso, fecha);
        if (bloques.isEmpty()) return new ArrayList<>();
        LocalDateTime desde = bloques.stream().map(Intervalo::inicio).min(Comparator.naturalOrder()).orElseThrow();
        LocalDateTime hasta = bloques.stream().map(Intervalo::fin).max(Comparator.naturalOrder()).orElseThrow();
        // Incluye ocupaciones iniciadas antes del día y las que empiezan después de medianoche.
        List<Turno> ocupados = turnoRepository.findTurnosOcupados(recursoId, desde, hasta, EstadoTurno.CONFIRMADO)
                .stream().sorted(Comparator.comparing(Turno::getFechaHoraInicio)).toList();

        Map<LocalDateTime, HorarioDisponibleDTO> horarios = new TreeMap<>();
        int duracion = servicio.getDuracionMinutos();
        LocalDateTime ahora = LocalDateTime.now();
        for (Intervalo bloque : bloques) {
            LocalDateTime puntero = bloque.inicio();
            for (Turno ocupado : ocupados) {
                if (!ocupado.getFechaHoraInicio().isBefore(bloque.fin())
                        || !ocupado.getFechaHoraFin().isAfter(bloque.inicio())) continue;
                while (!puntero.plusMinutes(duracion).isAfter(ocupado.getFechaHoraInicio())
                        && !puntero.plusMinutes(duracion).isAfter(bloque.fin())) {
                    agregarHorario(horarios, puntero, fecha, ahora);
                    puntero = puntero.plusMinutes(duracion);
                }
                if (puntero.isBefore(ocupado.getFechaHoraFin())) puntero = ocupado.getFechaHoraFin();
            }
            while (!puntero.plusMinutes(duracion).isAfter(bloque.fin())) {
                agregarHorario(horarios, puntero, fecha, ahora);
                puntero = puntero.plusMinutes(duracion);
            }
        }
        return new ArrayList<>(horarios.values());
    }

    private void agregarHorario(Map<LocalDateTime, HorarioDisponibleDTO> horarios, LocalDateTime inicio,
            LocalDate fecha, LocalDateTime ahora) {
        // Se filtra por la fecha de INICIO, sin truncar el fin de la reserva a medianoche.
        if (inicio.toLocalDate().equals(fecha) && inicio.isAfter(ahora)) {
            horarios.put(inicio, new HorarioDisponibleDTO(inicio.format(DateTimeFormatter.ofPattern("HH:mm")),
                    inicio.toString(), true));
        }
    }

    private void validarRecursoYServicio(Recurso recurso, Servicio servicio) {
        if (!Boolean.TRUE.equals(servicio.getActivo()) || servicio.getRecurso() == null) {
            throw new TurnoException(HttpStatus.NOT_FOUND, "El servicio no está disponible.");
        }
        if (!Objects.equals(servicio.getRecurso().getId(), recurso.getId())) {
            throw new TurnoException(HttpStatus.BAD_REQUEST, "El servicio no pertenece al recurso indicado.");
        }
        if (!Boolean.TRUE.equals(recurso.getActivo())) {
            throw new TurnoException(HttpStatus.NOT_FOUND, "El recurso no existe o está inactivo.");
        }
        if (recurso.getNegocio() == null || !Boolean.TRUE.equals(recurso.getNegocio().getActivo())) {
            throw new TurnoException(HttpStatus.CONFLICT, "El negocio está inactivo y no admite reservas.");
        }
        if (servicio.getDuracionMinutos() == null || servicio.getDuracionMinutos() <= 0) {
            throw new TurnoException(HttpStatus.BAD_REQUEST, "El servicio debe tener una duración positiva.");
        }
    }

    private void validarHorarioReserva(Recurso recurso, LocalDateTime inicio, LocalDateTime fin) {
        if (bloquesReserva(recurso, inicio.toLocalDate()).stream().noneMatch(b -> b.contiene(inicio, fin))) {
            throw new TurnoException(HttpStatus.BAD_REQUEST,
                    "El turno debe estar dentro de la jornada del recurso y del negocio, sin ocupar descansos.");
        }
    }

    private List<Intervalo> bloquesReserva(Recurso recurso, LocalDate fecha) {
        List<Intervalo> delRecurso = new ArrayList<>();
        List<Intervalo> delNegocio = new ArrayList<>();
        for (LocalDate dia : List.of(fecha.minusDays(1), fecha)) {
            if (recurso.getHorarios() != null) {
                recurso.getHorarios().stream()
                        .filter(h -> h.getDiaSemana() == dia.getDayOfWeek() && Boolean.TRUE.equals(h.getActivo()))
                        .forEach(h -> delRecurso.addAll(JornadaUtil.bloques(dia, h.getHoraInicio(), h.getHoraFin(),
                                h.getHoraInicioDescanso(), h.getHoraFinDescanso())));
            }
            if (recurso.getNegocio().getHorarios() != null) {
                recurso.getNegocio().getHorarios().stream()
                        .filter(h -> h.getDiaSemana() == dia.getDayOfWeek() && Boolean.TRUE.equals(h.getAbierto()))
                        .forEach(h -> delNegocio.addAll(JornadaUtil.bloques(dia, h.getHoraApertura(), h.getHoraCierre(),
                                h.getHoraInicioDescanso(), h.getHoraFinDescanso())));
            }
        }
        List<Intervalo> intersecciones = new ArrayList<>();
        for (Intervalo horarioRecurso : delRecurso) {
            for (Intervalo horarioNegocio : delNegocio) {
                Intervalo bloque = horarioRecurso.interseccion(horarioNegocio);
                if (bloque != null && bloque.inicio().isBefore(fecha.plusDays(1).atStartOfDay())
                        && bloque.fin().isAfter(fecha.atStartOfDay())) intersecciones.add(bloque);
            }
        }
        return intersecciones.stream().distinct().sorted(Comparator.comparing(Intervalo::inicio)).toList();
    }

    public ReporteResponseDTO generarReporteNegocio(Long negocioId, Long recursoId, LocalDateTime fechaInicio, LocalDateTime fechaFin, String emailLogueado) {
        Negocio negocio = negocioRepository.findByIdAndActivoTrue(negocioId)
                .orElseThrow(() -> new RuntimeException("El negocio no existe o está inactivo."));

        boolean esOwner = negocio.getOwner().getEmail().equals(emailLogueado);

        if (recursoId == null) {
            if (!esOwner) {
                throw new AccessDeniedException("Acceso denegado: Solo el dueño puede ver los reportes globales del negocio.");
            }
        } else {
            Recurso recurso = recursoRepository.findById(recursoId)
                    .orElseThrow(() -> new RuntimeException("El recurso no existe."));

            if (!Objects.equals(recurso.getNegocio().getId(), negocioId)) {
                throw new AccessDeniedException("Acceso denegado: El recurso no pertenece al negocio solicitado.");
            }
            PermisosRecurso.validarGestion(recurso, emailLogueado);
        }

        List<Turno> turnos;
        if (recursoId != null) {
            turnos = turnoRepository.findTurnosParaReportePorRecurso(negocioId, recursoId, fechaInicio, fechaFin);
        } else {
            turnos = turnoRepository.findTurnosParaReporte(negocioId, fechaInicio, fechaFin);
        }

        ReporteResponseDTO reporte = new ReporteResponseDTO();
        reporte.setTotalTurnos(turnos.size());

        if (turnos.isEmpty()) return reporte;

        int completados = 0;
        int pendientes = 0;
        int cancelados = 0;
        double ingresos = 0.0;
        LocalDateTime ahora = LocalDateTime.now();

        for (Turno t : turnos) {
            if (t.getEstado() == EstadoTurno.CANCELADO) {
                cancelados++;
            } else {
                if (t.getFechaHoraInicio().isBefore(ahora)) {
                    completados++;
                    ingresos += t.getServicio().getPrecio();
                } else {
                    pendientes++;
                }
            }
        }

        reporte.setTurnosCompletados(completados);
        reporte.setTurnosPendientes(pendientes);
        reporte.setTurnosCancelados(cancelados);
        reporte.setIngresosTotales(ingresos);

        double tasaCancelacion = cancelados * 100.0 / turnos.size();
        reporte.setTasaCancelacionPorcentaje(Math.round(tasaCancelacion * 100.0) / 100.0);

        Map<String, Long> servicios = turnos.stream()
                .filter(t -> t.getEstado() != EstadoTurno.CANCELADO)
                .collect(Collectors.groupingBy(t -> {
                    Servicio s = t.getServicio();
                    return s.getActivo() ? s.getNombre() : s.getNombre() + " (Inactivo)";
                }, Collectors.counting()));
        reporte.setServiciosPopulares(servicios);

        Map<String, Long> recursosPopulares = turnos.stream()
                .filter(t -> t.getEstado() != EstadoTurno.CANCELADO)
                .collect(Collectors.groupingBy(t -> {
                    Recurso r = t.getRecurso();
                    return r.getActivo() ? r.getNombre() : r.getNombre() + " (Inactivo)";
                }, Collectors.counting()));
        reporte.setRecursosPopulares(recursosPopulares);

        return reporte;
    }

    @Transactional(readOnly = true)
    public List<LocalDate> obtenerDiasLlenos(int anio, int mes, Long recursoId, Long servicioId) {
        List<LocalDate> diasLlenos = new ArrayList<>();
        LocalDate inicioMes = LocalDate.of(anio, mes, 1);
        LocalDate finMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());
        LocalDate hoy = LocalDate.now();

        // Si el mes pedido ya pasó, todos los días están inhabilitados
        if (finMes.isBefore(hoy)) return diasLlenos;

        // Empezamos a revisar desde hoy (no tiene sentido revisar días pasados de este mes)
        LocalDate fechaActual = inicioMes.isBefore(hoy) ? hoy : inicioMes;

        while (!fechaActual.isAfter(finMes)) {
            List<HorarioDisponibleDTO> horarios = obtenerHorariosDisponibles(fechaActual, recursoId, servicioId);
            if (horarios.isEmpty()) {
                diasLlenos.add(fechaActual);
            }
            fechaActual = fechaActual.plusDays(1);
        }
        return diasLlenos;
    }

    private void dispararEventoTurno(Turno turno, String tipoEvento) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fechaFormateada = turno.getFechaHoraInicio().format(formatter);

        TurnoEvent evento = new TurnoEvent(
                turno.getCliente().getEmail(),
                turno.getCliente().getNombre() + " " + turno.getCliente().getApellido(),
                turno.getRecurso().getNegocio().getNombre(),
                turno.getRecurso().getNegocio().getDireccion(),
                turno.getRecurso().getNombre(),
                turno.getServicio().getNombre(),
                fechaFormateada,
                tipoEvento
        );

        outboxService.encolar("notificaciones.exchange", "turno.cliente.routing.key", evento);
    }

    private void validarPermisosDeModificacion(Turno turno, String emailLogueado) {
        boolean esElCliente = turno.getCliente().getEmail().equals(emailLogueado);
        if (!esElCliente) {
            PermisosRecurso.validarGestion(turno.getRecurso(), emailLogueado);
        }
    }

}
