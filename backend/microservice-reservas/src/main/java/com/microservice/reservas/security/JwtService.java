package com.microservice.reservas.security; // Ajustá el paquete si lo pusiste en otro lado

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    // ⚠️ CLAVE SECRETA: En un entorno real esto va en el application.properties.
    // Tiene que ser un texto largo (mínimo 256 bits) para que el algoritmo sea seguro.
    private static final String SECRET_KEY = "ClaveSecretaMuyLargaYSuperSeguraParaElSistemaDeTurnos2026";

    // 1. Generar el Token (El "Pase VIP")
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername()) // Guardamos el email como identificador
                .issuedAt(new Date(System.currentTimeMillis())) // Fecha de creación
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // Expira en 24 horas
                .signWith(getSignInKey()) // Lo firmamos criptográficamente
                .compact();
    }

    // 2. Extraer el nombre de usuario (email) del Token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 3. Validar si el Token sigue siendo útil y pertenece al usuario
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // --- MÉTODOS AUXILIARES INTERNOS ---

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }
}