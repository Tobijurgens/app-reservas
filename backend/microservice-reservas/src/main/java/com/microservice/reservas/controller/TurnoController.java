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

    @GetMapping
    public ResponseEntity<List<TurnoResponseDTO>> obtenerTodos() {
        List<TurnoResponseDTO> lista = turnoService.obtenerTodos().stream()
                .map(TurnoResponseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TurnoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return turnoService.obtenerPorId(id)
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
            @RequestParam(defaultValue = "0") int x1,
            @RequestParam(defaultValue = "20") int x2
    ) {
        Pageable x3 = PageRequest.of(x1, x2);
        Page<TurnoResponseDTO> turnos = turnoService.obtenerTurnosPorNegocio(negocioId, x3)
                .map(TurnoResponseDTO::new);
        return ResponseEntity.ok(turnos);
    }

    // ... (Tu código existente) ...

    @GetMapping("/recurso/{recursoId}/historial")
    public ResponseEntity<Page<TurnoResponseDTO>> obtenerHistorialPorRecurso(
            @PathVariable Long recursoId,
            @RequestParam(defaultValue = "0") int x1,
            @RequestParam(defaultValue = "20") int x2,
            Principal principal) {

        Pageable x3 = PageRequest.of(x1, x2);
        Page<TurnoResponseDTO> turnos = turnoService.obtenerHistorialPorRecurso(recursoId, x3, principal.getName())
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.atTime(LocalTime.MAX);

        List<TurnoResponseDTO> lista = turnoService.obtenerTurnosOcupados(recursoId, inicioDia, finDia).stream()
                .map(TurnoResponseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/negocio/{negocioId}/reportes")
    public ResponseEntity<ReporteResponseDTO> obtenerReportes(
            @PathVariable Long negocioId,
            @RequestParam(required = false) Long recursoId, // NUEVO: Parámetro opcional
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime x1,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime x2,
            Principal principal) {

        return ResponseEntity.ok(turnoService.generarReporteNegocio(negocioId, recursoId, x1, x2, principal.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> cancelarTurno(@PathVariable Long id) {
        return ResponseEntity.ok(turnoService.cancelarTurno(id));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<TurnoResponseDTO> actualizarEstado(
            @PathVariable Long id,
            @RequestParam EstadoTurno nuevoEstado) {
        return ResponseEntity.ok(new TurnoResponseDTO(turnoService.actualizarEstado(id, nuevoEstado)));
    }

    // EL NUEVO ENDPOINT DEL CEREBRO
    @GetMapping("/disponibles")
    public ResponseEntity<List<HorarioDisponibleDTO>> obtenerHorariosDisponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam Long idRecurso,
            @RequestParam Long idServicio) {

        List<HorarioDisponibleDTO> disponibles = turnoService.obtenerHorariosDisponibles(fecha, idRecurso, idServicio);
        return ResponseEntity.ok(disponibles);
    }
}