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
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public Turno crearTurno(TurnoDTO turnoDto, String emailCliente) {
        if (turnoDto.getFechaHoraInicio().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("No se pueden reservar turnos en el pasado.");
        }

        Usuario cliente = usuarioRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (cliente.getEstadoCuenta() != EstadoCuenta.ACTIVO) {
            throw new RuntimeException("El cliente está inactivo");
        }

        Servicio servicio = servicioRepository.findByIdAndActivoTrue(turnoDto.getIdServicio())
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado o inactivo"));

        Recurso recurso = servicio.getRecurso();

        if (!recurso.getActivo()) {
            throw new RuntimeException("El recurso asociado a este servicio está inactivo");
        }

        LocalDateTime fechaInicio = turnoDto.getFechaHoraInicio();
        LocalDateTime fechaFin = fechaInicio.plusMinutes(servicio.getDuracionMinutos());
        EstadoTurno estadoIgnorado = EstadoTurno.CANCELADO;

        boolean ocupado = turnoRepository.existeSolapamiento(recurso.getId(), fechaInicio, fechaFin, estadoIgnorado);
        if (ocupado) {
            throw new RuntimeException("El horario seleccionado ya no se encuentra disponible. Por favor, elige otro.");
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

        rabbitTemplate.convertAndSend("notificaciones.exchange", "avisos.internos.routing.key",
                new AvisoInternoEvent(recurso.getNegocio().getOwner().getEmail(), "Nuevo Turno", nombreCliente + " reservó en " + nombreNegocio + "con " + recurso.getNombre()));

        if (recurso.getUsuario() != null && !recurso.getUsuario().getEmail().equals(recurso.getNegocio().getOwner().getEmail())) {
            rabbitTemplate.convertAndSend("notificaciones.exchange", "avisos.internos.routing.key",
                    new AvisoInternoEvent(recurso.getUsuario().getEmail(), "Nuevo Turno Asignado", nombreCliente + " reservó con vos en " + nombreNegocio));
        }
        return turnoGuardado;
    }

    @Transactional
    public Turno actualizarEstado(Long id, EstadoTurno nuevoEstado, String emailLogueado) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El turno especificado no existe."));

        validarPermisosDeModificacion(turno, emailLogueado);

        turno.setEstado(nuevoEstado);
        return turnoRepository.save(turno);
    }

    public Page<Turno> obtenerTurnosPorNegocio(Long negocioId, Pageable pageable, String emailLogueado) {
        Negocio negocio = negocioRepository.findByIdAndActivoTrue(negocioId)
                .orElseThrow(() -> new RuntimeException("El negocio no existe."));

        if (!negocio.getOwner().getEmail().equals(emailLogueado)) {
            throw new RuntimeException("Acceso denegado: Solo el dueño puede ver todos los turnos del negocio.");
        }
        return turnoRepository.findByNegocioIdDesc(negocioId, pageable);
    }

    public Page<Turno> obtenerHistorialPorRecurso(Long recursoId, Pageable pageable, String emailLogueado) {
        Recurso recurso = recursoRepository.findByIdAndActivoTrue(recursoId)
                .orElseThrow(() -> new RuntimeException("El recurso no existe."));

        validarPermisosAcceso(recurso, emailLogueado);
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

        validarPermisosAcceso(recurso, emailLogueado);
        return turnoRepository.findByRecursoId(recursoId);
    }

    public List<Turno> obtenerTurnosOcupados(Long recursoId, LocalDateTime inicioDia, LocalDateTime finDia, String emailLogueado) {
        Recurso recurso = recursoRepository.findByIdAndActivoTrue(recursoId)
                .orElseThrow(() -> new RuntimeException("El recurso no existe."));

        validarPermisosAcceso(recurso, emailLogueado);
        return turnoRepository.findTurnosOcupados(recursoId, inicioDia, finDia, EstadoTurno.CANCELADO);
    }

    @Transactional
    public String cancelarTurno(Long id, String emailLogueado) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El turno no existe."));

        if (turno.getEstado() == EstadoTurno.CANCELADO) {
            throw new RuntimeException("El turno ya se encontraba cancelado.");
        }

        validarPermisosDeModificacion(turno, emailLogueado);

        turno.setEstado(EstadoTurno.CANCELADO);
        Turno turnoCancelado = turnoRepository.save(turno);

        dispararEventoTurno(turnoCancelado, "CANCELACION");

        String nombreCliente = turno.getCliente().getNombre() + " " + turno.getCliente().getApellido();

        // Aviso al dueño
        rabbitTemplate.convertAndSend("notificaciones.exchange", "avisos.internos.routing.key",
                new AvisoInternoEvent(turno.getRecurso().getNegocio().getOwner().getEmail(), "Turno Cancelado", nombreCliente + " canceló su turno."));

        // Aviso al recurso
        if (turno.getRecurso().getUsuario() != null && !turno.getRecurso().getUsuario().getEmail().equals(turno.getRecurso().getNegocio().getOwner().getEmail())) {
            rabbitTemplate.convertAndSend("notificaciones.exchange", "avisos.internos.routing.key",
                    new AvisoInternoEvent(turno.getRecurso().getUsuario().getEmail(), "Turno Cancelado", nombreCliente + " canceló su turno con vos."));
        }

        return "Turno cancelado correctamente.";
    }

    public List<HorarioDisponibleDTO> obtenerHorariosDisponibles(LocalDate fecha, Long recursoId, Long servicioId) {
        if (fecha.isBefore(LocalDate.now())) {
            return new ArrayList<>();
        }

        Recurso recurso = recursoRepository.findByIdAndActivoTrue(recursoId)
                .orElseThrow(() -> new RuntimeException("Recurso no encontrado"));
        Servicio servicio = servicioRepository.findByIdAndActivoTrue(servicioId)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        DayOfWeek diaHoy = fecha.getDayOfWeek();
        DayOfWeek diaAyer = fecha.minusDays(1).getDayOfWeek();

        HorarioRecurso hRecursoHoy = recurso.getHorarios().stream()
                .filter(h -> h.getDiaSemana() == diaHoy && h.getActivo())
                .findFirst().orElse(null);

        HorarioRecurso hRecursoAyer = recurso.getHorarios().stream()
                .filter(h -> h.getDiaSemana() == diaAyer && h.getActivo())
                .findFirst().orElse(null);

        List<BloqueTiempo> bloquesCompletos = new ArrayList<>();

        // 1. Armar la línea de tiempo real de AYER
        if (hRecursoAyer != null) {
            bloquesCompletos.addAll(generarBloquesAbsolutos(fecha.minusDays(1), hRecursoAyer, true));
        }

        // 2. Armar la línea de tiempo real de HOY
        if (hRecursoHoy != null) {
            bloquesCompletos.addAll(generarBloquesAbsolutos(fecha, hRecursoHoy, false));
        }

        // 3. Filtrar con un "molde": Solo nos quedamos con los pedazos de tiempo que caen exactamente en el día que el cliente tocó (de 00:00 a 23:59)
        LocalDateTime inicioDiaFiltro = fecha.atStartOfDay();
        LocalDateTime finDiaFiltro = fecha.atTime(LocalTime.MAX);

        List<BloqueTiempo> bloquesEfectivos = new ArrayList<>();
        for (BloqueTiempo b : bloquesCompletos) {
            LocalDateTime maxInicio = b.inicio.isAfter(inicioDiaFiltro) ? b.inicio : inicioDiaFiltro;
            LocalDateTime minFin = b.fin.isBefore(finDiaFiltro) ? b.fin : finDiaFiltro;

            // Si hay intersección válida, lo guardamos
            if (maxInicio.isBefore(minFin)) {
                bloquesEfectivos.add(new BloqueTiempo(maxInicio, minFin, b.esDeAyer));
            }
        }

        // Ordenamos estrictamente por orden cronológico de los bloques
        bloquesEfectivos.sort(Comparator.comparing(b -> b.inicio));

        List<HorarioDisponibleDTO> horarios = new ArrayList<>();
        int duracionServicio = servicio.getDuracionMinutos();
        DateTimeFormatter formatterHora = DateTimeFormatter.ofPattern("HH:mm");

        for (BloqueTiempo bloque : bloquesEfectivos) {
            List<Turno> turnosDelBloque = turnoRepository.findTurnosOcupados(
                    recursoId, bloque.inicio, bloque.fin, EstadoTurno.CANCELADO
            );
            turnosDelBloque.sort(Comparator.comparing(Turno::getFechaHoraInicio));

            LocalDateTime puntero = bloque.inicio;

            for (Turno turnoOcupado : turnosDelBloque) {
                LocalDateTime inicioTurnoOcupado = turnoOcupado.getFechaHoraInicio();
                LocalDateTime finTurnoOcupado = turnoOcupado.getFechaHoraFin();

                while (!puntero.plusMinutes(duracionServicio).isAfter(inicioTurnoOcupado)) {
                    if (!puntero.plusMinutes(duracionServicio).isAfter(bloque.fin)) {
                        horarios.add(new HorarioDisponibleDTO(puntero.format(formatterHora), puntero.toString(), true));
                    }
                    puntero = puntero.plusMinutes(duracionServicio);
                }
                if (puntero.isBefore(finTurnoOcupado)) {
                    puntero = finTurnoOcupado;
                }
            }

            while (!puntero.plusMinutes(duracionServicio).isAfter(bloque.fin)) {
                horarios.add(new HorarioDisponibleDTO(puntero.format(formatterHora), puntero.toString(), true));
                puntero = puntero.plusMinutes(duracionServicio);
            }
        }

        // Eliminamos los duplicados generados por la lógica de solapamiento
        List<HorarioDisponibleDTO> horariosUnicos = horarios.stream()
                .distinct()
                .collect(Collectors.toList());

        // Eliminamos los turnos que ya pasaron el día de hoy
        LocalDateTime ahora = LocalDateTime.now();
        horariosUnicos.removeIf(h -> LocalDateTime.parse(h.getFechaHoraISO()).isBefore(ahora));

        // 🔥 ORDEN FINAL ESTRICTO POR FECHA Y HORA ABSOLUTA 🔥
        // Esto garantiza que 2026-09-25T22:00 aparezca ANTES que 2026-09-26T00:00
        horariosUnicos.sort(Comparator.comparing(h -> LocalDateTime.parse(h.getFechaHoraISO())));

        return horariosUnicos;
    }

    /**
     * Convierte las horas sueltas (LocalTime) en Fechas y Horas exactas (LocalDateTime).
     * Si detecta que una hora de fin es menor a la de inicio, automáticamente asume que es del día siguiente.
     */
    private List<BloqueTiempo> generarBloquesAbsolutos(LocalDate fechaReferencia, HorarioRecurso hr, boolean esDeAyer) {
        List<BloqueTiempo> bloques = new ArrayList<>();

        LocalDateTime inicioReal = fechaReferencia.atTime(hr.getHoraInicio());
        LocalDateTime finReal = fechaReferencia.atTime(hr.getHoraFin());

        // Si cierra más temprano de lo que abre, es porque cruzó la medianoche
        if (hr.getHoraFin().isBefore(hr.getHoraInicio())) {
            finReal = finReal.plusDays(1);
        }

        // Si tiene descanso, partimos el bloque en dos
        if (hr.getHoraInicioDescanso() != null && hr.getHoraFinDescanso() != null) {
            LocalDateTime iDescansoReal = fechaReferencia.atTime(hr.getHoraInicioDescanso());
            LocalDateTime fDescansoReal = fechaReferencia.atTime(hr.getHoraFinDescanso());

            // Acomodamos los días del descanso si cruzaron la medianoche
            if (hr.getHoraInicioDescanso().isBefore(hr.getHoraInicio())) iDescansoReal = iDescansoReal.plusDays(1);
            if (hr.getHoraFinDescanso().isBefore(hr.getHoraInicio())) fDescansoReal = fDescansoReal.plusDays(1);

            bloques.add(new BloqueTiempo(inicioReal, iDescansoReal, esDeAyer));
            bloques.add(new BloqueTiempo(fDescansoReal, finReal, esDeAyer));
        } else {
            // Si no hay descanso, es un solo bloque de corrido
            bloques.add(new BloqueTiempo(inicioReal, finReal, esDeAyer));
        }

        return bloques;
    }

    public ReporteResponseDTO generarReporteNegocio(Long negocioId, Long recursoId, LocalDateTime fechaInicio, LocalDateTime fechaFin, String emailLogueado) {
        Negocio negocio = negocioRepository.findByIdAndActivoTrue(negocioId)
                .orElseThrow(() -> new RuntimeException("El negocio no existe o está inactivo."));

        boolean esOwner = negocio.getOwner().getEmail().equals(emailLogueado);

        if (!esOwner) {
            if (recursoId == null) {
                throw new RuntimeException("Acceso denegado: Solo el dueño puede ver los reportes globales del negocio.");
            }
            Recurso recurso = recursoRepository.findById(recursoId)
                    .orElseThrow(() -> new RuntimeException("El recurso no existe."));

            if (recurso.getUsuario() == null || !recurso.getUsuario().getEmail().equals(emailLogueado)) {
                throw new RuntimeException("Acceso denegado: Solo podés ver tus propios reportes.");
            }
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

        rabbitTemplate.convertAndSend("notificaciones.exchange", "turno.cliente.routing.key", evento);
    }

    private void validarPermisosDeModificacion(Turno turno, String emailLogueado) {
        boolean esElCliente = turno.getCliente().getEmail().equals(emailLogueado);
        boolean esElOwner = turno.getRecurso().getNegocio().getOwner().getEmail().equals(emailLogueado);
        boolean esElRecurso = turno.getRecurso().getUsuario() != null &&
                turno.getRecurso().getUsuario().getEmail().equals(emailLogueado);

        if (!esElCliente && !esElOwner && !esElRecurso) {
            throw new RuntimeException("Acceso denegado: No tenés permisos para cancelar o modificar este turno.");
        }
    }

    private void validarPermisosAcceso(Recurso recurso, String emailLogueado) {
        String emailOwner = recurso.getNegocio().getOwner().getEmail();
        boolean esElOwner = emailOwner.equals(emailLogueado);
        boolean esElRecurso = recurso.getUsuario() != null && recurso.getUsuario().getEmail().equals(emailLogueado);

        if (!esElOwner && !esElRecurso) {
            throw new RuntimeException("Acceso denegado: No tenés permisos para ver esta información.");
        }
        if (esElRecurso && !esElOwner && recurso.getEstadoAsociacion() != com.microservice.reservas.enums.EstadoAsociacion.ACTIVO) {
            throw new RuntimeException("Acceso denegado: Debés aceptar la invitación en tu correo antes de poder ver la agenda.");
        }
    }

    // Clase auxiliar para reemplazar al viejo Rango
    private static class BloqueTiempo {
        LocalDateTime inicio;
        LocalDateTime fin;
        boolean esDeAyer;

        BloqueTiempo(LocalDateTime inicio, LocalDateTime fin, boolean esDeAyer) {
            this.inicio = inicio;
            this.fin = fin;
            this.esDeAyer = esDeAyer;
        }
    }
}