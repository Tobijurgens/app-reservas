package com.microservice.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 1. Permitimos el envío de credenciales (Cookies, Headers de Autorización)
        config.setAllowCredentials(true);

        // 2. Origen exacto de tu frontend (Vite)
        config.addAllowedOrigin("http://localhost:5173");

        // 3. Permitimos todos los headers y métodos HTTP
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        // 4. Aplicamos esta regla a todas las rutas que pasen por el Gateway
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}