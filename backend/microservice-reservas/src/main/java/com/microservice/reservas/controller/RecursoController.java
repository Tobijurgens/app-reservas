package com.microservice.reservas.controller;

import com.microservice.reservas.dto.request.RecursoDTO;
import com.microservice.reservas.dto.response.RecursoResponseDTO;
import com.microservice.reservas.service.RecursoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recursos")
@RequiredArgsConstructor
public class RecursoController {

    private final RecursoService recursoService;

    @PostMapping
    public ResponseEntity<RecursoResponseDTO> crearRecurso(@Valid @RequestBody RecursoDTO recursoDto, Principal principal) {
        String emailLogueado = principal.getName();
        return ResponseEntity.ok(new RecursoResponseDTO(recursoService.crearRecurso(recursoDto, emailLogueado)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecursoResponseDTO> actualizarRecurso(
            @PathVariable Long id,
            @Valid @RequestBody RecursoDTO recursoDto,
            Principal principal) {
        String emailLogueado = principal.getName();
        return ResponseEntity.ok(new RecursoResponseDTO(recursoService.actualizarRecurso(id, recursoDto, emailLogueado)));
    }

    @GetMapping
    public ResponseEntity<List<RecursoResponseDTO>> obtenerTodos() {
        List<RecursoResponseDTO> lista = recursoService.obtenerTodos().stream()
                .map(RecursoResponseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecursoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return recursoService.obtenerPorId(id)
                .map(recurso -> ResponseEntity.ok(new RecursoResponseDTO(recurso)))
                .orElseThrow(() -> new RuntimeException("Error: Recurso no encontrado o inactivo."));
    }

    @GetMapping("/negocio/{negocioId}")
    public ResponseEntity<List<RecursoResponseDTO>> obtenerPorNegocio(@PathVariable Long negocioId) {
        List<RecursoResponseDTO> lista = recursoService.obtenerPorNegocio(negocioId).stream()
                .map(RecursoResponseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarRecurso(@PathVariable Long id, Principal principal) {
        String emailLogueado = principal.getName();
        return ResponseEntity.ok(recursoService.eliminarRecurso(id, emailLogueado));
    }
}