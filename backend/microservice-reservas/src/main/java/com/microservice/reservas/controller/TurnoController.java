package com.microservice.reservas.controller;

import com.microservice.reservas.dto.request.TurnoDTO;
import com.microservice.reservas.dto.response.ReporteResponseDTO;
import com.microservice.reservas.dto.response.TurnoResponseDTO;
import com.microservice.reservas.dto.response.HorarioDisponibleDTO;
import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.service.TurnoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;

    @PostMapping
    public ResponseEntity<TurnoResponseDTO> crearTurno(@Valid @RequestBody TurnoDTO turnoDto, Principal principal) {
        String emailCliente = principal.getName();
        return ResponseEntity.ok(new TurnoResponseDTO(turnoService.crearTurno(turnoDto, emailCliente)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TurnoResponseDTO> obtenerPorId(@PathVariable Long id, Principal principal) {
        return turnoService.obtenerPorId(id, principal.getName())
                .map(turno -> ResponseEntity.ok(new TurnoResponseDTO(turno)))
                .orElseThrow(() -> new RuntimeException("Error: Turno no encontrado."));
    }

    @GetMapping("/mis-turnos")
    public ResponseEntity<List<TurnoResponseDTO>> obtenerMisTurnos(Principal principal) {
        String emailCliente = principal.getName();
        List<TurnoResponseDTO> lista = turnoService.obtenerMisTurnos(emailCliente).stream()
                .map(TurnoResponseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/negocio/{negocioId}")
    public ResponseEntity<Page<TurnoResponseDTO>> obtenerTurnosPorNegocio(
            @PathVariable Long negocioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TurnoResponseDTO> turnos = turnoService.obtenerTurnosPorNegocio(negocioId, pageable, principal.getName())
                .map(TurnoResponseDTO::new);
        return ResponseEntity.ok(turnos);
    }

    @GetMapping("/recurso/{recursoId}/historial")
    public ResponseEntity<Page<TurnoResponseDTO>> obtenerHistorialPorRecurso(
            @PathVariable Long recursoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TurnoResponseDTO> turnos = turnoService.obtenerHistorialPorRecurso(recursoId, pageable, principal.getName())
                .map(TurnoResponseDTO::new);
        return ResponseEntity.ok(turnos);
    }

    @GetMapping("/agenda/{recursoId}")
    public ResponseEntity<List<TurnoResponseDTO>> obtenerAgendaRecurso(@PathVariable Long recursoId, Principal principal) {
        String emailLogueado = principal.getName();
        List<TurnoResponseDTO> lista = turnoService.obtenerAgendaRecurso(recursoId, emailLogueado).stream()
                .map(TurnoResponseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/recurso/{recursoId}/disponibilidad")
    public ResponseEntity<List<TurnoResponseDTO>> obtenerTurnosOcupados(
            @PathVariable Long recursoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Principal principal) {

        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.atTime(LocalTime.MAX);

        List<TurnoResponseDTO> lista = turnoService.obtenerTurnosOcupados(recursoId, inicioDia, finDia, principal.getName()).stream()
                .map(TurnoResponseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/negocio/{negocioId}/reportes")
    public ResponseEntity<ReporteResponseDTO> obtenerReportes(
            @PathVariable Long negocioId,
            @RequestParam(required = false) Long recursoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            Principal principal) {

        return ResponseEntity.ok(turnoService.generarReporteNegocio(negocioId, recursoId, fechaInicio, fechaFin, principal.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> cancelarTurno(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(turnoService.cancelarTurno(id, principal.getName()));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<TurnoResponseDTO> actualizarEstado(
            @PathVariable Long id,
            @RequestParam EstadoTurno nuevoEstado,
            Principal principal) {
        return ResponseEntity.ok(new TurnoResponseDTO(turnoService.actualizarEstado(id, nuevoEstado, principal.getName())));
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<HorarioDisponibleDTO>> obtenerHorariosDisponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam Long idRecurso,
            @RequestParam Long idServicio) {

        List<HorarioDisponibleDTO> disponibles = turnoService.obtenerHorariosDisponibles(fecha, idRecurso, idServicio);
        return ResponseEntity.ok(disponibles);
    }

    @GetMapping("/dias-llenos")
    public ResponseEntity<List<LocalDate>> obtenerDiasLlenos(
            @RequestParam int anio,
            @RequestParam int mes,
            @RequestParam Long idRecurso,
            @RequestParam Long idServicio) {
        return ResponseEntity.ok(turnoService.obtenerDiasLlenos(anio, mes, idRecurso, idServicio));
    }
}