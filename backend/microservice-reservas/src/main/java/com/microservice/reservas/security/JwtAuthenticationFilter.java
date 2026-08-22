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

    // --- Evita que el filtro actúe en rutas públicas ---
    @Override
    protected boolean shouldNotFilter(@Nonnull HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/");
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

        // 1. Si no hay header "Authorization" o no arranca con "Bearer ", lo dejamos pasar al siguiente filtro
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Recortamos los primeros 7 caracteres ("Bearer ") para quedarnos solo con el Token limpio
        jwt = authHeader.substring(7);

        // 3. NUEVO: Evitamos que el frontend nos mande la palabra "null" literal o texto vacío
        if (jwt.trim().isEmpty() || jwt.equals("null") || jwt.equals("undefined")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. NUEVO: Bloque try-catch fundamental. Si el token expiró o está malformado, lo atajamos acá.
        try {
            // Extraemos el email del Token
            userEmail = jwtService.extractUsername(jwt);

            // Si hay email y el usuario no está ya autenticado en este proceso
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Vamos a buscar sus datos
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // Validamos el Token
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // Creamos la credencial interna de Spring
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Lo guardamos en el contexto (¡Le abrimos la puerta!)
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Si el token es inválido o expiró, simplemente logueamos el error y el filtro sigue.
            // Spring Security, al no encontrar la autenticación en el SecurityContext, devolverá un 403 Forbidden.
            logger.error("Error validando el token JWT: " + e.getMessage());
        }

        // Que siga su camino hacia el controlador (o hacia el rechazo si no se autenticó)
        filterChain.doFilter(request, response);
    }
}