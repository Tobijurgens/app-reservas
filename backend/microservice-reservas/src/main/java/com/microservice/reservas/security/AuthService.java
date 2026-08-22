package com.microservice.reservas.security;

import com.microservice.reservas.dto.request.auth.LoginRequest;
import com.microservice.reservas.dto.request.auth.RegisterRequest;
import com.microservice.reservas.entity.TokenSeguridad;
import com.microservice.reservas.entity.Usuario;
import com.microservice.reservas.enums.EstadoCuenta;
import com.microservice.reservas.enums.TipoToken;
import com.microservice.reservas.repository.TokenSeguridadRepository;
import com.microservice.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j // Agregamos SLF4J de Lombok para un logging profesional
@Service
@RequiredArgsConstructor // Lombok crea el constructor automáticamente con los campos 'final'
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final TokenSeguridadRepository tokenSeguridadRepository; // ¡ACÁ ESTÁ LA SOLUCIÓN!
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public String register(RegisterRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElse(new Usuario());

        if (usuario.getId() != null && usuario.getEstadoCuenta() == EstadoCuenta.ACTIVO) {
            throw new RuntimeException("El email ya se encuentra registrado y activo.");
        }

        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setEmail(request.getEmail());
        usuario.setTelefono(request.getTelefono());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setEstadoCuenta(EstadoCuenta.PENDIENTE_VERIFICACION);

        usuario = usuarioRepository.save(usuario);

        TokenSeguridad token = tokenSeguridadRepository.findByUsuarioIdAndTipo(usuario.getId(), TipoToken.VERIFICACION)
                .orElse(new TokenSeguridad());

        token.setToken(UUID.randomUUID().toString());
        token.setTipo(TipoToken.VERIFICACION);
        token.setFechaExpiracion(LocalDateTime.now().plusMinutes(30));
        token.setUsuario(usuario);

        tokenSeguridadRepository.save(token);

        // Reemplazamos el System.out.println por un log profesional
        log.info("PREPARANDO EVENTO PARA RABBITMQ. TOKEN GENERADO: {}", token.getToken());

        return "Usuario registrado. Por favor, revisá tu casilla de correo para verificar la cuenta.";
    }

    public String login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Solo dejamos loguear a los que ya validaron la cuenta
        Usuario usuario = usuarioRepository.findByEmailAndEstadoCuenta(request.getEmail(), EstadoCuenta.ACTIVO)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado o cuenta sin verificar."));

        return jwtService.generateToken(usuario);
    }
}