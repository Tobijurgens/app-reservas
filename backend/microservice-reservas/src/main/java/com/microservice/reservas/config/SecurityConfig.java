package com.microservice.reservas.config;

import com.microservice.reservas.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter){
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Lo que ya teníamos: Auth público
                        .requestMatchers("/api/auth/**").permitAll()

                        // 2. GETs públicos: Cualquiera puede ver negocios, recursos y servicios
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/negocios/**", "/api/recursos/**", "/api/servicios/**", "/api/turnos/disponibles").permitAll()

                        // Rutas públicas de Swagger
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // 3. ¡EL RESTO EXIGE TOKEN OBLIGATORIO!
                        .anyRequest().authenticated()
                )
                // 4. Importante: Agregamos el filtro que valida el Token en cada petición
                .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Este Bean enseña a Spring a encriptar contraseñas antes de guardarlas en Postgres
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}