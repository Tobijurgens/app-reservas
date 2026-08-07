package com.microservice.reservas.controller;

import com.microservice.reservas.dto.TurnoDTO;
import com.microservice.reservas.entity.Recurso;
import com.microservice.reservas.entity.Servicio;
import com.microservice.reservas.entity.Turno;
import com.microservice.reservas.entity.Usuario;
import com.microservice.reservas.repository.RecursoRepository;
import com.microservice.reservas.repository.ServicioRepository;
import com.microservice.reservas.repository.TurnoRepository;
import com.microservice.reservas.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/turnos")
public class TurnoController {

    @Autowired
    private TurnoRepository turnoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RecursoRepository recursoRepository;
    @Autowired
    private ServicioRepository servicioRepository;

    @PostMapping
    public ResponseEntity <Object> crearTurno(@RequestBody TurnoDTO turnoDto) {
        if (turnoDto.getFechaHoraInicio() == null) {
            return ResponseEntity.badRequest().body("Error: La fecha y hora de inicio son obligatorias.");
        }

        // Validación estricta: Las 3 partes deben estar ACTIVAS
        Optional <Usuario> clienteOpt = usuarioRepository.findByIdAndActivoTrue(turnoDto.getIdCliente());
        if (clienteOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: Cliente no encontrado o inactivo.");
        }

        Optional <Recurso> recursoOpt = recursoRepository.findByIdAndActivoTrue(turnoDto.getIdRecurso());
        if (recursoOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: Recurso no encontrado o inactivo.");
        }

        Optional <Servicio> servicioOpt = servicioRepository.findByIdAndActivoTrue(turnoDto.getIdServicio());
        if (servicioOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: Servicio no encontrado o inactivo.");
        }

        Servicio servicio = servicioOpt.get();

        LocalDateTime inicio = turnoDto.getFechaHoraInicio();
        LocalDateTime fin = inicio.plusMinutes(servicio.getDuracionMinutos());

        boolean ocupado = turnoRepository.existeSolapamiento(recursoOpt.get().getId(), inicio, fin);
        if (ocupado) {
            return ResponseEntity.badRequest().body("Error: El recurso ya tiene un turno reservado que interfiere con este horario.");
        }

        Turno turno = new Turno();
        turno.setFechaHoraInicio(inicio);
        turno.setFechaHoraFin(fin);
        turno.setEstado("PENDIENTE");

        turno.setCliente(clienteOpt.get());
        turno.setRecurso(recursoOpt.get());
        turno.setServicio(servicio);

        Turno nuevoTurno = turnoRepository.save(turno);
        return ResponseEntity.ok(nuevoTurno);
    }

    @GetMapping
    public ResponseEntity<List<Turno>> obtenerTodos() {
        return ResponseEntity.ok(turnoRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> obtenerPorId(@PathVariable Long id) {
        return turnoRepository.findById(id)
                .map(turno -> ResponseEntity.ok((Object) turno))
                .orElseGet(() -> ResponseEntity.badRequest().body("Error: Turno no encontrado."));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<Turno>> obtenerTurnosPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(turnoRepository.findByClienteId(clienteId));
    }

    @GetMapping("/recurso/{recursoId}/disponibilidad")
    public ResponseEntity<List<Turno>> obtenerTurnosOcupados(
            @PathVariable Long recursoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.atTime(LocalTime.MAX);
        return ResponseEntity.ok(turnoRepository.findTurnosOcupados(recursoId, inicioDia, finDia));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> cancelarTurno(@PathVariable Long id) {
        Optional<Turno> turnoOpt = turnoRepository.findById(id);

        if (turnoOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: El turno no existe.");
        }

        Turno turno = turnoOpt.get();

        if (turno.getEstado().equals("CANCELADO")) {
            return ResponseEntity.badRequest().body("Error: El turno ya se encontraba cancelado.");
        }

        turno.setEstado("CANCELADO");
        turnoRepository.save(turno);

        return ResponseEntity.ok("Turno cancelado correctamente.");
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Object> actualizarEstado(
            @PathVariable Long id,
            @RequestParam String nuevoEstado) {

        // 1. Buscamos si el turno existe
        Optional<Turno> turnoOpt = turnoRepository.findById(id);
        if (turnoOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: El turno especificado no existe.");
        }

        // 2. Validamos que el estado ingresado sea uno de los permitidos
        // (Para evitar que alguien mande estados inventados como "HOLA")
        if (!nuevoEstado.equals("CONFIRMADO") &&
                !nuevoEstado.equals("CANCELADO") &&
                !nuevoEstado.equals("PENDIENTE")) {
            return ResponseEntity.badRequest().body("Error: Estado no válido. Use PENDIENTE, CONFIRMADO o CANCELADO.");
        }

        // 3. Aplicamos el cambio y guardamos
        Turno turno = turnoOpt.get();
        turno.setEstado(nuevoEstado);
        turnoRepository.save(turno);

        return ResponseEntity.ok(turno);
    }
}

