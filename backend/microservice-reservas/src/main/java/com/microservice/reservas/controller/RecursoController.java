
package com.microservice.reservas.controller;

import com.microservice.reservas.dto.RecursoDTO;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/recursos")
public class RecursoController {

    @Autowired
    private RecursoRepository recursoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NegocioRepository negocioRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @PostMapping
    public ResponseEntity<Object> crearRecurso(@RequestBody RecursoDTO recursoDto) {
        // Validación: El negocio debe existir Y estar activo
        Optional <Negocio> negocioOpt = negocioRepository.findByIdAndActivoTrue(recursoDto.getIdNegocio());
        if (negocioOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: El negocio especificado no existe o está inactivo.");
        }

        Recurso recurso = new Recurso();
        recurso.setNombre(recursoDto.getNombre());
        recurso.setDescripcion(recursoDto.getDescripcion());
        recurso.setNegocio(negocioOpt.get());
        recurso.setActivo(true); // Nace activo

        if (recursoDto.getEmailUsuario() != null && !recursoDto.getEmailUsuario().isEmpty()) {
            Optional <Usuario> usuarioOpt = usuarioRepository.findByEmailAndActivoTrue(recursoDto.getEmailUsuario());
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Error: No se encontró ningún usuario activo con el email " + recursoDto.getEmailUsuario());
            }
            recurso.setUsuario(usuarioOpt.get());
        }

        Recurso nuevoRecurso = recursoRepository.save(recurso);
        return ResponseEntity.ok(nuevoRecurso);
    }

    @GetMapping
    public ResponseEntity<List<Recurso>> obtenerTodos() {
        return ResponseEntity.ok(recursoRepository.findByActivoTrue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> obtenerPorId(@PathVariable Long id) {
        return recursoRepository.findByIdAndActivoTrue(id)
                .map(recurso -> ResponseEntity.ok((Object) recurso))
                .orElseGet(() -> ResponseEntity.badRequest().body("Error: Recurso no encontrado o inactivo."));
    }

    @GetMapping("/negocio/{negocioId}")
    public ResponseEntity<List<Recurso>> obtenerPorNegocio(@PathVariable Long negocioId) {
        return ResponseEntity.ok(recursoRepository.findByNegocioIdAndActivoTrue(negocioId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminarRecurso(@PathVariable Long id) {
        Optional<Recurso> recursoOpt = recursoRepository.findById(id);
        if (recursoOpt.isEmpty()) return ResponseEntity.badRequest().body("Error: El recurso no existe.");

        Recurso recurso = recursoOpt.get();
        if (!recurso.getActivo()) return ResponseEntity.badRequest().body("Error: Ya estaba eliminado.");

        // PASO 1: Dar de baja el Recurso
        recurso.setActivo(false);
        recursoRepository.save(recurso);

        // PASO 2: Dar de baja (borrado lógico) todos sus Servicios activos
        List<Servicio> serviciosDelRecurso = servicioRepository.findByRecursoIdAndActivoTrue(id);
        for (Servicio servicio : serviciosDelRecurso) {
            servicio.setActivo(false);
            servicioRepository.save(servicio);
        }

        // PASO 3: Cancelar solo los turnos a futuro (PENDIENTE o CONFIRMADO)
        List<String> estadosACancelar = Arrays.asList("PENDIENTE", "CONFIRMADO");
        List<Turno> turnosAfectados = turnoRepository.findByRecursoIdAndEstadoIn(id, estadosACancelar);

        for (Turno turno : turnosAfectados) {
            turno.setEstado("CANCELADO");
            turnoRepository.save(turno);
        }

        // Mensaje de éxito detallado
        return ResponseEntity.ok("Recurso dado de baja exitosamente. Se eliminaron " +
                serviciosDelRecurso.size() + " servicios y se cancelaron " +
                turnosAfectados.size() + " turnos pendientes.");
    }
}
