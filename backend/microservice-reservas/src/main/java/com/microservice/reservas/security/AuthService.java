package com.microservice.reservas.security;

import com.microservice.reservas.dto.request.auth.LoginRequest;
import com.microservice.reservas.dto.request.auth.RegisterRequest;
import com.microservice.reservas.entity.Usuario;
import com.microservice.reservas.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    // Registrar o reactivar un usuario
    @Transactional
    public String register(RegisterRequest request) {
        // 1. Buscamos el usuario. Si no existe, creamos una instancia nueva al vuelo.
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElse(new Usuario());

        // 2. Si el usuario ya existía en BD (tiene ID) y está activo, lo frenamos.
        if (usuario.getId() != null && usuario.getActivo()) {
            throw new RuntimeException("El email ya se encuentra registrado y activo.");
        }

        // 3. Seteamos los datos (este bloque único sirve para ambos casos)
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setEmail(request.getEmail()); // Si era nuevo lo necesita, si ya existía simplemente lo pisa con el mismo
        usuario.setTelefono(request.getTelefono());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setActivo(true);

        usuarioRepository.save(usuario);

        return jwtService.generateToken(usuario);
    }

    // Loguear un usuario existente
    public String login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return jwtService.generateToken(usuario);
    }
}