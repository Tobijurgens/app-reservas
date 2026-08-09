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
    public ResponseEntity<UsuarioResponseDTO> obtenerPorId(@PathVariable Long id) {
        return usuarioService.obtenerPorId(id)
                .map(usuario -> ResponseEntity.ok(new UsuarioResponseDTO(usuario)))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado o inactivo."));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UsuarioResponseDTO> obtenerPorEmail(@PathVariable String email) {
        return usuarioService.obtenerPorEmail(email)
                .map(usuario -> ResponseEntity.ok(new UsuarioResponseDTO(usuario)))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado o inactivo."));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarUsuario(@PathVariable Long id, Principal principal) {
        String emailLogueado = principal.getName();
        return ResponseEntity.ok(usuarioService.eliminarUsuario(id, emailLogueado));
    }
}