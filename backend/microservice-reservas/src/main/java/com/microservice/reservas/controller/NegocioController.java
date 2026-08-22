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

    // NUEVO: Endpoint para el Dashboard del dueño
    @GetMapping("/mis-negocios")
    public ResponseEntity<List<NegocioResponseDTO>> obtenerMisNegocios(Principal principal) {
        // Spring Security saca el email del Token JWT automáticamente
        String emailLogueado = principal.getName();

        List<NegocioResponseDTO> misNegocios = negocioService.obtenerMisNegocios(emailLogueado).stream()
                .map(NegocioResponseDTO::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(misNegocios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NegocioResponseDTO> obtenerPorId(@PathVariable Long id) {
        return negocioService.obtenerPorId(id)
                .map(negocio -> ResponseEntity.ok(new NegocioResponseDTO(negocio)))
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado o inactivo."));
    }

    // NUEVO: Endpoint público para consultar por el Slug/Alias
    @GetMapping("/publico/{alias}")
    public ResponseEntity<NegocioResponseDTO> obtenerPorAlias(@PathVariable String alias) {
        return negocioService.obtenerPorAlias(alias)
                .map(negocio -> ResponseEntity.ok(new NegocioResponseDTO(negocio)))
                .orElseThrow(() -> new RuntimeException("El negocio no existe o no se encuentra disponible."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarNegocio(@PathVariable Long id, Principal principal) {
        String emailLogueado = principal.getName();
        return ResponseEntity.ok(negocioService.eliminarNegocio(id, emailLogueado));
    }
}