package com.microservice.reservas.service;

import com.microservice.reservas.dto.request.TurnoDTO;
import com.microservice.reservas.dto.response.HorarioDisponibleDTO;
import com.microservice.reservas.dto.response.ReporteResponseDTO;
import com.microservice.reservas.enums.EstadoCuenta;
import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // IMPORTANTE

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

    // NUEVO: Agregamos @Transactional para evitar la concurrencia (overbooking)
    @Transactional
    public Turno crearTurno(TurnoDTO turnoDto, String emailCliente) {
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

        LocalDateTime x1 = turnoDto.getFechaHoraInicio();
        LocalDateTime x2 = x1.plusMinutes(servicio.getDuracionMinutos());
        EstadoTurno x3 = EstadoTurno.CANCELADO;

        // Utilizamos tus variables x1, x2 y x3
        boolean ocupado = turnoRepository.existeSolapamiento(recurso.getId(), x1, x2, x3);
        if (ocupado) {
            throw new RuntimeException("El horario seleccionado ya no se encuentra disponible. Por favor, elegí otro.");
        }

        Turno turno = new Turno();
        turno.setFechaHoraInicio(x1);
        turno.setFechaHoraFin(x2);
        turno.setEstado(EstadoTurno.CONFIRMADO);
        turno.setCliente(cliente);
        turno.setRecurso(recurso);
        turno.setServicio(servicio);

        return turnoRepository.save(turno);
    }

    public Turno actualizarEstado(Long id, EstadoTurno nuevoEstado) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El turno especificado no existe."));

        turno.setEstado(nuevoEstado);
        return turnoRepository.save(turno);
    }

    public List<Turno> obtenerTodos() {
        return turnoRepository.findAll();
    }

    public Page<Turno> obtenerTurnosPorNegocio(Long negocioId, Pageable x3) {
        return turnoRepository.findByNegocioIdDesc(negocioId, x3);
    }

    public Page<Turno> obtenerHistorialPorRecurso(Long recursoId, Pageable x3, String emailLogueado) {
        Recurso recurso = recursoRepository.findByIdAndActivoTrue(recursoId)
                .orElseThrow(() -> new RuntimeException("El recurso no existe."));

        validarPermisosAcceso(recurso, emailLogueado);

        return turnoRepository.findByRecursoIdDesc(recursoId, x3);
    }

    public Optional<Turno> obtenerPorId(Long id) {
        return turnoRepository.findById(id);
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

    public List<Turno> obtenerTurnosOcupados(Long recursoId, LocalDateTime inicioDia, LocalDateTime finDia) {
        return turnoRepository.findTurnosOcupados(recursoId, inicioDia, finDia, EstadoTurno.CANCELADO);
    }

    public String cancelarTurno(Long id) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El turno no existe."));

        if (turno.getEstado() == EstadoTurno.CANCELADO) {
            throw new RuntimeException("El turno ya se encontraba cancelado.");
        }

        turno.setEstado(EstadoTurno.CANCELADO);
        turnoRepository.save(turno);
        return "Turno cancelado correctamente.";
    }

    // EL CEREBRO: ALGORITMO DE INTERSECCIÓN Y ANCLAJE DINÁMICO
    public List<HorarioDisponibleDTO> obtenerHorariosDisponibles(LocalDate fecha, Long recursoId, Long servicioId) {
        Recurso recurso = recursoRepository.findByIdAndActivoTrue(recursoId)
                .orElseThrow(() -> new RuntimeException("Recurso no encontrado"));
        Negocio negocio = recurso.getNegocio();
        Servicio servicio = servicioRepository.findByIdAndActivoTrue(servicioId)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        DayOfWeek dia = fecha.getDayOfWeek();

        HorarioNegocio hNegocio = negocio.getHorarios().stream()
                .filter(h -> h.getDiaSemana() == dia && h.getAbierto())
                .findFirst().orElse(null);

        if (hNegocio == null) return new ArrayList<>();

        HorarioRecurso hRecurso = recurso.getHorarios().stream()
                .filter(h -> h.getDiaSemana() == dia && h.getActivo())
                .findFirst().orElse(null);

        if (hRecurso == null) return new ArrayList<>();

        List<Rango> bloquesEfectivos = calcularInterseccion(hNegocio, hRecurso);

        List<HorarioDisponibleDTO> horarios = new ArrayList<>();
        int duracionServicio = servicio.getDuracionMinutos();
        DateTimeFormatter formatterHora = DateTimeFormatter.ofPattern("HH:mm");

        for (Rango bloque : bloquesEfectivos) {
            LocalDateTime inicioBloque = fecha.atTime(bloque.inicio);
            LocalDateTime finBloque = fecha.atTime(bloque.fin);

            List<Turno> turnosDelBloque = turnoRepository.findTurnosOcupados(
                    recursoId, inicioBloque, finBloque, EstadoTurno.CANCELADO
            );
            turnosDelBloque.sort(Comparator.comparing(Turno::getFechaHoraInicio));

            LocalDateTime puntero = inicioBloque;

            for (Turno turnoOcupado : turnosDelBloque) {
                LocalDateTime inicioTurnoOcupado = turnoOcupado.getFechaHoraInicio();
                LocalDateTime finTurnoOcupado = turnoOcupado.getFechaHoraFin();

                while (!puntero.plusMinutes(duracionServicio).isAfter(inicioTurnoOcupado)) {
                    horarios.add(new HorarioDisponibleDTO(puntero.format(formatterHora), puntero.toString(), true));
                    puntero = puntero.plusMinutes(duracionServicio);
                }

                if (puntero.isBefore(finTurnoOcupado)) {
                    puntero = finTurnoOcupado;
                }
            }

            while (!puntero.plusMinutes(duracionServicio).isAfter(finBloque)) {
                horarios.add(new HorarioDisponibleDTO(puntero.format(formatterHora), puntero.toString(), true));
                puntero = puntero.plusMinutes(duracionServicio);
            }
        }

        // --- NUEVA BARRERA DE TIEMPO (EVITAR RESERVAS EN EL PASADO) ---
        LocalDate hoy = LocalDate.now();
        if (fecha.equals(hoy)) {
            LocalTime ahora = LocalTime.now();
            horarios.removeIf(h -> {
                LocalTime horaDelTurno = LocalTime.parse(h.getHora());
                return horaDelTurno.isBefore(ahora);
            });
        }

        return horarios;
    }

    // --- MÉTODOS PRIVADOS ---

    private void validarPermisosAcceso(Recurso recurso, String emailLogueado) {
        String emailOwner = recurso.getNegocio().getOwner().getEmail();
        boolean esElOwner = emailOwner.equals(emailLogueado);
        boolean esElRecurso = recurso.getUsuario() != null && recurso.getUsuario().getEmail().equals(emailLogueado);

        if (!esElOwner && !esElRecurso) {
            throw new RuntimeException("Acceso denegado: No tenés permisos para ver esta información.");
        }
    }

    private List<Rango> calcularInterseccion(HorarioNegocio hn, HorarioRecurso hr) {
        List<Rango> rangosNegocio = armarRangos(hn.getHoraApertura(), hn.getHoraCierre(), hn.getHoraInicioDescanso(), hn.getHoraFinDescanso());
        List<Rango> rangosRecurso = armarRangos(hr.getHoraInicio(), hr.getHoraFin(), hr.getHoraInicioDescanso(), hr.getHoraFinDescanso());

        List<Rango> interseccion = new ArrayList<>();

        for (Rango rn : rangosNegocio) {
            for (Rango rr : rangosRecurso) {
                LocalTime maxInicio = rn.inicio.isAfter(rr.inicio) ? rn.inicio : rr.inicio;
                LocalTime minFin = rn.fin.isBefore(rr.fin) ? rn.fin : rr.fin;

                if (maxInicio.isBefore(minFin)) {
                    interseccion.add(new Rango(maxInicio, minFin));
                }
            }
        }
        return interseccion;
    }

    private List<Rango> armarRangos(LocalTime inicio, LocalTime fin, LocalTime inicioDescanso, LocalTime finDescanso) {
        List<Rango> rangos = new ArrayList<>();
        if (inicioDescanso != null && finDescanso != null && inicioDescanso.isBefore(finDescanso)) {
            if (inicio.isBefore(inicioDescanso)) rangos.add(new Rango(inicio, inicioDescanso));
            if (finDescanso.isBefore(fin)) rangos.add(new Rango(finDescanso, fin));
        } else {
            rangos.add(new Rango(inicio, fin));
        }
        return rangos;
    }

    private static class Rango {
        LocalTime inicio;
        LocalTime fin;
        Rango(LocalTime inicio, LocalTime fin) { this.inicio = inicio; this.fin = fin; }
    }

    public ReporteResponseDTO generarReporteNegocio(Long negocioId, Long recursoId, LocalDateTime x1, LocalDateTime x2, String emailLogueado) {
        Negocio negocio = negocioRepository.findByIdAndActivoTrue(negocioId)
                .orElseThrow(() -> new RuntimeException("El negocio no existe o está inactivo."));

        boolean esOwner = negocio.getOwner().getEmail().equals(emailLogueado);

        // SEGURIDAD: Control de acceso para Dueño vs Empleado
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
            turnos = turnoRepository.findTurnosParaReportePorRecurso(negocioId, recursoId, x1, x2);
        } else {
            turnos = turnoRepository.findTurnosParaReporte(negocioId, x1, x2);
        }

        ReporteResponseDTO reporte = new ReporteResponseDTO();
        reporte.setTotalTurnos(turnos.size());

        if (turnos.isEmpty()) {
            return reporte;
        }

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

        // Cálculo de tasa de cancelación limpio y sin casteos redundantes
        double tasaCancelacion = cancelados * 100.0 / turnos.size();
        reporte.setTasaCancelacionPorcentaje(Math.round(tasaCancelacion * 100.0) / 100.0);

        // Agrupación de Servicios (Añade etiqueta si fue eliminado)
        Map<String, Long> servicios = turnos.stream()
                .filter(t -> t.getEstado() != EstadoTurno.CANCELADO)
                .collect(Collectors.groupingBy(t -> {
                    Servicio s = t.getServicio();
                    return s.getActivo() ? s.getNombre() : s.getNombre() + " (Inactivo)";
                }, Collectors.counting()));
        reporte.setServiciosPopulares(servicios);

        // Agrupación de Recursos (Añade etiqueta si el empleado fue despedido/eliminado)
        Map<String, Long> recursosPopulares = turnos.stream()
                .filter(t -> t.getEstado() != EstadoTurno.CANCELADO)
                .collect(Collectors.groupingBy(t -> {
                    Recurso r = t.getRecurso();
                    return r.getActivo() ? r.getNombre() : r.getNombre() + " (Inactivo)";
                }, Collectors.counting()));
        reporte.setRecursosPopulares(recursosPopulares);

        return reporte;
    }
}