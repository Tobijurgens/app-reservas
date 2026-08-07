package com.microservice.reservas.controller;

import com.microservice.reservas.dto.NegocioDTO;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/negocios")
public class NegocioController {

    @Autowired
    private NegocioRepository negocioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RecursoRepository recursoRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @PostMapping
    public ResponseEntity <Object> crearNegocio(@RequestBody NegocioDTO negocioDto) {
        // Validación: El usuario dueño debe existir Y estar activo
        Optional <Usuario> ownerOpt = usuarioRepository.findByIdAndActivoTrue(negocioDto.getIdOwner());

        if (ownerOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: El usuario especificado para ser dueño no existe o está inactivo.");
        }

        Negocio negocio = new Negocio();
        negocio.setNombre(negocioDto.getNombre());
        negocio.setDireccion(negocioDto.getDireccion());
        negocio.setTelefono(negocioDto.getTelefono());
        negocio.setOwner(ownerOpt.get());
        negocio.setActivo(true); // Nace activo por defecto

        Negocio nuevoNegocio = negocioRepository.save(negocio);
        return ResponseEntity.ok(nuevoNegocio);
    }

    @GetMapping
    public ResponseEntity<List<Negocio>> obtenerTodos() {
        return ResponseEntity.ok(negocioRepository.findByActivoTrue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> obtenerPorId(@PathVariable Long id) {
        return negocioRepository.findByIdAndActivoTrue(id)
                .map(negocio -> ResponseEntity.ok((Object) negocio))
                .orElseGet(() -> ResponseEntity.badRequest().body("Error: Negocio no encontrado o inactivo."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminarNegocio(@PathVariable Long id) {
        Optional<Negocio> negocioOpt = negocioRepository.findById(id);
        if (negocioOpt.isEmpty()) return ResponseEntity.badRequest().body("Error: El negocio no existe.");

        Negocio negocio = negocioOpt.get();
        if (!negocio.getActivo()) return ResponseEntity.badRequest().body("Error: Ya estaba eliminado.");

        // 1. Dar de baja el Negocio
        negocio.setActivo(false);
        negocioRepository.save(negocio);

        int recursosBorrados = 0;
        int serviciosBorrados = 0;
        int turnosCancelados = 0;
        List<String> estadosACancelar = Arrays.asList("PENDIENTE", "CONFIRMADO");

        // 2. Buscar todos los recursos de este negocio
        List<Recurso> recursos = recursoRepository.findByNegocioIdAndActivoTrue(id);

        for (Recurso recurso : recursos) {
            recurso.setActivo(false);
            recursoRepository.save(recurso);
            recursosBorrados++;

            // 3. Buscar y dar de baja los servicios de este recurso
            List<Servicio> servicios = servicioRepository.findByRecursoIdAndActivoTrue(recurso.getId());
            for (Servicio servicio : servicios) {
                servicio.setActivo(false);
                servicioRepository.save(servicio);
                serviciosBorrados++;
            }

            // 4. Cancelar los turnos futuros asignados a este recurso
            List<Turno> turnos = turnoRepository.findByRecursoIdAndEstadoIn(recurso.getId(), estadosACancelar);
            for (Turno turno : turnos) {
                turno.setEstado("CANCELADO");
                turnoRepository.save(turno);
                turnosCancelados++;
            }
        }

        return ResponseEntity.ok("Negocio dado de baja exitosamente. Se eliminaron " + recursosBorrados +
                " recursos, " + serviciosBorrados + " servicios y se cancelaron " +
                turnosCancelados + " turnos pendientes.");
    }

}