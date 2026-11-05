package com.microservice.reservas.controller;

import com.microservice.reservas.dto.request.auth.LoginRequest;
import com.microservice.reservas.dto.request.auth.RegisterRequest;
import com.microservice.reservas.dto.response.AuthResponse;
import com.microservice.reservas.security.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        String token = authService.register(request);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @GetMapping("/verificar")
    public ResponseEntity<String> verificarCuenta(@RequestParam("token") String token) {
        try {
            String resultado = authService.verificarCuenta(token);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}