package com.microservice.reservas.security;

import com.microservice.reservas.dto.request.auth.LoginRequest;
import com.microservice.reservas.dto.request.auth.RegisterRequest;
import com.microservice.reservas.dto.event.UsuarioRegistradoEvent;
import com.microservice.reservas.entity.TokenSeguridad;
import com.microservice.reservas.entity.Usuario;
import com.microservice.reservas.enums.EstadoCuenta;
import com.microservice.reservas.enums.TipoToken;
import com.microservice.reservas.repository.TokenSeguridadRepository;
import com.microservice.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final TokenSeguridadRepository tokenSeguridadRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RabbitTemplate rabbitTemplate;

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

        UsuarioRegistradoEvent evento = new UsuarioRegistradoEvent(
                usuario.getEmail(),
                usuario.getNombre(),
                token.getToken()
        );

        rabbitTemplate.convertAndSend("notificaciones.exchange", "registro.email.routing.key", evento);

        log.info("🚀 Evento de registro disparado a RabbitMQ para: {}", usuario.getEmail());

        return "Usuario registrado. Por favor, revisá tu casilla de correo para verificar la cuenta.";
    }

    public String login(LoginRequest request) {
        Usuario usuarioVerificador = usuarioRepository.findByEmail(request.getEmail()).orElse(null);

        if (usuarioVerificador != null && usuarioVerificador.getEstadoCuenta() == EstadoCuenta.SUSPENDIDO) {
            throw new RuntimeException("Bad credentials");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        Usuario usuarioActivo = usuarioRepository.findByEmailAndEstadoCuenta(request.getEmail(), EstadoCuenta.ACTIVO)
                .orElseThrow(() -> new RuntimeException("Bad credentials"));

        return jwtService.generateToken(usuarioActivo);
    }

    @Transactional
    public String verificarCuenta(String tokenStr) {
        TokenSeguridad token = tokenSeguridadRepository.findByToken(tokenStr)
                .orElseThrow(() -> new RuntimeException("Token inválido o no encontrado."));

        if (token.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            tokenSeguridadRepository.delete(token);
            throw new RuntimeException("El token ha expirado. Por favor, registrate nuevamente.");
        }

        Usuario usuario = token.getUsuario();
        usuario.setEstadoCuenta(EstadoCuenta.ACTIVO);
        usuarioRepository.save(usuario);

        tokenSeguridadRepository.delete(token);

        log.info("✅ Cuenta activada con éxito para el usuario: {}", usuario.getEmail());

        return "¡Cuenta verificada y activada con éxito! Ya podés iniciar sesión en AgendApp.";
    }
}