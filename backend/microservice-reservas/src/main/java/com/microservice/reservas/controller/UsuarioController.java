package com.microservice.reservas.controller;

import com.microservice.reservas.dto.request.ActualizarPerfilRequest;
import com.microservice.reservas.dto.response.UsuarioResponseDTO;
import com.microservice.reservas.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/perfil")
    public ResponseEntity<UsuarioResponseDTO> obtenerMiPerfil(Principal principal) {
        return ResponseEntity.ok(new UsuarioResponseDTO(usuarioService.obtenerMiPerfil(principal.getName())));
    }

    @PutMapping("/perfil")
    public ResponseEntity<UsuarioResponseDTO> actualizarPerfil(@Valid @RequestBody ActualizarPerfilRequest request, Principal principal) {
        String emailLogueado = principal.getName();
        return ResponseEntity.ok(new UsuarioResponseDTO(usuarioService.actualizarPerfil(emailLogueado, request)));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> obtenerTodos() {
        List<UsuarioResponseDTO> lista = usuarioService.obtenerTodos().stream()
                .map(UsuarioResponseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerPorId(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(new UsuarioResponseDTO(usuarioService.obtenerPorId(id, principal.getName())));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UsuarioResponseDTO> obtenerPorEmail(@PathVariable String email, Principal principal) {
        return ResponseEntity.ok(new UsuarioResponseDTO(usuarioService.obtenerPorEmail(email, principal.getName())));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarUsuario(@PathVariable Long id, Principal principal) {
        String emailLogueado = principal.getName();
        return ResponseEntity.ok(usuarioService.eliminarUsuario(id, emailLogueado));
    }
}
