package com.microservice.reservas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.Nonnull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Si no hay header "Authorization" o no arranca con "Bearer ", lo dejamos pasar al siguiente filtro (ej: login)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Recortamos los primeros 7 caracteres ("Bearer ") para quedarnos solo con el Token limpio
        jwt = authHeader.substring(7);

        // 3. Extraemos el email del Token
        userEmail = jwtService.extractUsername(jwt);

        // 4. Si hay email y el usuario no está ya autenticado en este proceso
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Vamos a buscar sus datos (esto lo configuramos en el siguiente paso)
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 5. Validamos el Token
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // 6. Creamos la credencial interna de Spring
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. Lo guardamos en el contexto (¡Le abrimos la puerta!)
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 8. Que siga su camino hacia el controlador
        filterChain.doFilter(request, response);
    }
}