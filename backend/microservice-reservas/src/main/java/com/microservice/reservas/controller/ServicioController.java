package com.microservice.reservas.controller;

import com.microservice.reservas.dto.ServicioDTO;
import com.microservice.reservas.entity.Recurso;
import com.microservice.reservas.entity.Servicio;
import com.microservice.reservas.entity.Turno;
import com.microservice.reservas.repository.RecursoRepository;
import com.microservice.reservas.repository.ServicioRepository;
import com.microservice.reservas.repository.TurnoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private RecursoRepository recursoRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @PostMapping
    public ResponseEntity<Object> crearServicio(@RequestBody ServicioDTO servicioDto) {
        // Validación: El recurso debe existir Y estar activo
        Optional<Recurso> recursoOpt = recursoRepository.findByIdAndActivoTrue(servicioDto.getIdRecurso());

        if (recursoOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: El recurso especificado no existe o está inactivo.");
        }

        Servicio servicio = new Servicio();
        servicio.setNombre(servicioDto.getNombre());
        servicio.setDescripcion(servicioDto.getDescripcion());
        servicio.setPrecio(servicioDto.getPrecio());
        servicio.setDuracionMinutos(servicioDto.getDuracionMinutos());
        servicio.setRecurso(recursoOpt.get());
        servicio.setActivo(true); // Nace activo

        Servicio nuevoServicio = servicioRepository.save(servicio);
        return ResponseEntity.ok(nuevoServicio);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizarServicio(
            @PathVariable Long id,
            @RequestBody ServicioDTO servicioDto) {

        // 1. Buscamos si el servicio existe
        Optional<Servicio> servicioOpt = servicioRepository.findById(id);
        if (servicioOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: El servicio especificado no existe.");
        }

        // 2. Traemos el objeto de la base de datos
        Servicio servicio = servicioOpt.get();

        // 3. Actualizamos solo los campos que tienen sentido cambiar
        // (Por lo general, no se permite cambiar de a qué Recurso pertenece, si se equivocaron es mejor borrarlo y crearlo de nuevo)
        servicio.setNombre(servicioDto.getNombre());
        servicio.setDescripcion(servicioDto.getDescripcion());
        servicio.setPrecio(servicioDto.getPrecio());
        servicio.setDuracionMinutos(servicioDto.getDuracionMinutos());

        // 4. Guardamos los cambios
        Servicio servicioActualizado = servicioRepository.save(servicio);
        return ResponseEntity.ok(servicioActualizado);
    }

    @GetMapping
    public ResponseEntity<List<Servicio>> obtenerTodos() {
        return ResponseEntity.ok(servicioRepository.findByActivoTrue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> obtenerPorId(@PathVariable Long id) {
        return servicioRepository.findByIdAndActivoTrue(id)
                .map(servicio -> ResponseEntity.ok((Object) servicio))
                .orElseGet(() -> ResponseEntity.badRequest().body("Error: Servicio no encontrado o inactivo."));
    }

    @GetMapping("/recurso/{recursoId}")
    public ResponseEntity<List<Servicio>> obtenerPorRecurso(@PathVariable Long recursoId) {
        return ResponseEntity.ok(servicioRepository.findByRecursoIdAndActivoTrue(recursoId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminarServicio(@PathVariable Long id) {
        Optional<Servicio> servicioOpt = servicioRepository.findById(id);
        if (servicioOpt.isEmpty()) return ResponseEntity.badRequest().body("Error: El servicio no existe.");

        Servicio servicio = servicioOpt.get();
        if (!servicio.getActivo()) return ResponseEntity.badRequest().body("Error: Ya estaba eliminado.");

        // 1. Dar de baja el servicio
        servicio.setActivo(false);
        servicioRepository.save(servicio);

        // 2. Cancelar turnos futuros asociados a ESTE servicio
        List<String> estadosACancelar = Arrays.asList("PENDIENTE", "CONFIRMADO");
        List<Turno> turnosAfectados = turnoRepository.findByServicioIdAndEstadoIn(id, estadosACancelar);

        for (Turno turno : turnosAfectados) {
            turno.setEstado("CANCELADO");
            turnoRepository.save(turno);
        }

        return ResponseEntity.ok("Servicio eliminado lógicamente. Se cancelaron " + turnosAfectados.size() + " turnos pendientes.");
    }

}