package com.microservice.reservas.controller;

import com.microservice.reservas.dto.request.NegocioDTO;
import com.microservice.reservas.dto.response.NegocioResponseDTO;
import com.microservice.reservas.service.NegocioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/negocios")
@RequiredArgsConstructor
public class NegocioController {

    private final NegocioService negocioService;

    @PostMapping
    public ResponseEntity<NegocioResponseDTO> crearNegocio(@Valid @RequestBody NegocioDTO negocioDto, Principal principal) {
        String emailOwner = principal.getName();
        var nuevoNegocio = negocioService.crearNegocio(negocioDto, emailOwner);
        return ResponseEntity.ok(new NegocioResponseDTO(nuevoNegocio));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NegocioResponseDTO> actualizarNegocio(
            @PathVariable Long id,
            @Valid @RequestBody NegocioDTO negocioDto,
            Principal principal) {
        String emailLogueado = principal.getName();
        return ResponseEntity.ok(new NegocioResponseDTO(negocioService.actualizarNegocio(id, negocioDto, emailLogueado)));
    }

    @GetMapping
    public ResponseEntity<List<NegocioResponseDTO>> obtenerTodos() {
        List<NegocioResponseDTO> lista = negocioService.obtenerTodos().stream()
                .map(NegocioResponseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NegocioResponseDTO> obtenerPorId(@PathVariable Long id) {
        return negocioService.obtenerPorId(id)
                .map(negocio -> ResponseEntity.ok(new NegocioResponseDTO(negocio)))
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado o inactivo."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarNegocio(@PathVariable Long id, Principal principal) {
        String emailLogueado = principal.getName();
        return ResponseEntity.ok(negocioService.eliminarNegocio(id, emailLogueado));
    }
}