package com.microservice.reservas.controller;

import com.microservice.reservas.dto.request.ServicioDTO;
import com.microservice.reservas.dto.response.ServicioResponseDTO;
import com.microservice.reservas.service.ServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioService servicioService;

    @PostMapping
    public ResponseEntity<ServicioResponseDTO> crearServicio(@Valid @RequestBody ServicioDTO servicioDto, Principal principal) {
        String emailLogueado = principal.getName();
        return ResponseEntity.ok(new ServicioResponseDTO(servicioService.crearServicio(servicioDto, emailLogueado)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicioResponseDTO> actualizarServicio(@PathVariable Long id, @Valid @RequestBody ServicioDTO servicioDto, Principal principal) {
        String emailLogueado = principal.getName();
        return ResponseEntity.ok(new ServicioResponseDTO(servicioService.actualizarServicio(id, servicioDto, emailLogueado)));
    }

    @GetMapping
    public ResponseEntity<List<ServicioResponseDTO>> obtenerTodos() {
        List<ServicioResponseDTO> lista = servicioService.obtenerTodos().stream()
                .map(ServicioResponseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioResponseDTO> obtenerPorId(@PathVariable Long id) {
        return servicioService.obtenerPorId(id)
                .map(servicio -> ResponseEntity.ok(new ServicioResponseDTO(servicio)))
                .orElseThrow(() -> new RuntimeException("Error: Servicio no encontrado o inactivo."));
    }

    @GetMapping("/recurso/{recursoId}")
    public ResponseEntity<List<ServicioResponseDTO>> obtenerPorRecurso(@PathVariable Long recursoId) {
        List<ServicioResponseDTO> lista = servicioService.obtenerPorRecurso(recursoId).stream()
                .map(ServicioResponseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarServicio(@PathVariable Long id, Principal principal) {
        String emailLogueado = principal.getName();
        return ResponseEntity.ok(servicioService.eliminarServicio(id, emailLogueado));
    }
}