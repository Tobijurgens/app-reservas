package com.microservice.notificaciones.controller;

import com.microservice.notificaciones.config.SecurityConfig;
import com.microservice.notificaciones.entity.HistorialNotificacion;
import com.microservice.notificaciones.enums.EstadoNotificacion;
import com.microservice.notificaciones.enums.TipoNotificacion;
import com.microservice.notificaciones.repository.HistorialNotificacionRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificacionController.class)
@Import(SecurityConfig.class)
class NotificacionSecurityTest {

    private static final String EMAIL_A = "a@example.com";
    private static final String EMAIL_B = "b@example.com";
    // Clave efímera exclusiva de esta prueba; no usa secretos ni infraestructura reales.
    private static final String TEST_SECRET = UUID.randomUUID().toString() + UUID.randomUUID();
    private static final SecretKey KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> TEST_SECRET);
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private HistorialNotificacionRepository repository;

    @ParameterizedTest
    @ValueSource(strings = {"/api/notificaciones", "/api/notificaciones/no-leidas/count"})
    void rechazaConsultasAnonimasInclusoConEmailFalso(String endpoint) throws Exception {
        mvc.perform(get(endpoint).header("X-User-Email", EMAIL_B))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/notificaciones/1/leer", "/api/notificaciones/leer-todas"})
    void rechazaModificacionesAnonimas(String endpoint) throws Exception {
        mvc.perform(put(endpoint).header("X-User-Email", EMAIL_B))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-jwt", "", "null", "undefined", "a.b.c"})
    void rechazaTokensInvalidos(String token) throws Exception {
        mvc.perform(get("/api/notificaciones").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }

    @Test
    void rechazaFirmaDeOtraClave() throws Exception {
        String token = Jwts.builder().subject(EMAIL_A)
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Jwts.SIG.HS512.key().build()).compact();
        assertRejected(token);
    }

    @Test
    void rechazaJwtVencido() throws Exception {
        assertRejected(token(EMAIL_A, Instant.now().minusSeconds(60)));
    }

    @Test
    void rechazaJwtSinFirma() throws Exception {
        assertRejected(Jwts.builder().subject(EMAIL_A)
                .expiration(Date.from(Instant.now().plusSeconds(300))).compact());
    }

    @Test
    void rechazaJwtSinExpiracion() throws Exception {
        assertRejected(Jwts.builder().subject(EMAIL_A).signWith(KEY).compact());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void rechazaJwtSinIdentidad(String subject) throws Exception {
        assertRejected(token(subject, Instant.now().plusSeconds(300)));
    }

    @Test
    void consultaSoloNotificacionesPropiasAunqueEnvieEmailAjeno() throws Exception {
        var propia = notificacion(1L, EMAIL_A);
        var ajena = notificacion(2L, EMAIL_B);
        when(repository.findByEmailDestinatarioOrderByFechaEnvioDesc(anyString()))
                .thenAnswer(call -> EMAIL_A.equals(call.getArgument(0)) ? List.of(propia) : List.of(ajena));

        mvc.perform(get("/api/notificaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(EMAIL_A))
                        .header("X-User-Email", EMAIL_B)
                        .param("email", EMAIL_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Aviso"))
                .andExpect(jsonPath("$[0].mensaje").value("Mensaje"))
                .andExpect(jsonPath("$[0].fechaEnvio").exists())
                .andExpect(jsonPath("$[0].leida").value(false))
                .andExpect(jsonPath("$[0].soloInApp").value(true))
                .andExpect(jsonPath("$[0].detalleError").doesNotExist())
                .andExpect(jsonPath("$[0].emailDestinatario").doesNotExist());

        verify(repository).findByEmailDestinatarioOrderByFechaEnvioDesc(EMAIL_A);
        verify(repository, never()).findByEmailDestinatarioOrderByFechaEnvioDesc(EMAIL_B);
    }

    @Test
    void usuarioBPuedeConsultarSuPropioHistorial() throws Exception {
        when(repository.findByEmailDestinatarioOrderByFechaEnvioDesc(EMAIL_B))
                .thenReturn(List.of(notificacion(2L, EMAIL_B)));

        mvc.perform(get("/api/notificaciones").header(HttpHeaders.AUTHORIZATION, bearer(EMAIL_B)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    void cuentaSoloNoLeidasPropiasAunqueEnvieEmailAjeno() throws Exception {
        when(repository.countByEmailDestinatarioAndLeidaFalse(EMAIL_A)).thenReturn(3L);

        mvc.perform(get("/api/notificaciones/no-leidas/count")
                        .header(HttpHeaders.AUTHORIZATION, bearer(EMAIL_A))
                        .header("X-User-Email", EMAIL_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));
        verify(repository).countByEmailDestinatarioAndLeidaFalse(EMAIL_A);
        verify(repository, never()).countByEmailDestinatarioAndLeidaFalse(EMAIL_B);
    }

    @Test
    void marcaUnaNotificacionPropiaSinCabeceraDeEmail() throws Exception {
        var propia = notificacion(1L, EMAIL_A);
        when(repository.findByIdAndEmailDestinatario(1L, EMAIL_A)).thenReturn(Optional.of(propia));

        mvc.perform(put("/api/notificaciones/1/leer").header(HttpHeaders.AUTHORIZATION, bearer(EMAIL_A)))
                .andExpect(status().isOk());

        assertTrue(propia.isLeida());
        verify(repository).save(propia);
    }

    @Test
    void noPuedeMarcarNotificacionAjenaAunqueEnvieSuEmail() throws Exception {
        when(repository.findByIdAndEmailDestinatario(2L, EMAIL_A)).thenReturn(Optional.empty());

        mvc.perform(put("/api/notificaciones/2/leer")
                        .header(HttpHeaders.AUTHORIZATION, bearer(EMAIL_A))
                        .header("X-User-Email", EMAIL_B))
                .andExpect(status().isNotFound());

        verify(repository).findByIdAndEmailDestinatario(2L, EMAIL_A);
        verify(repository, never()).findByIdAndEmailDestinatario(2L, EMAIL_B);
        verify(repository, never()).save(any());
    }

    @Test
    void marcaTodasSolamenteParaLaIdentidadDelJwt() throws Exception {
        mvc.perform(put("/api/notificaciones/leer-todas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(EMAIL_A))
                        .header("X-User-Email", EMAIL_B))
                .andExpect(status().isOk());
        verify(repository).marcarTodasComoLeidas(EMAIL_A);
        verify(repository, never()).marcarTodasComoLeidas(EMAIL_B);
    }

    @ParameterizedTest
    @ValueSource(strings = {"HS256", "HS384", "HS512"})
    void aceptaAlgoritmosHmacUtilizadosPorReservas(String algorithm) throws Exception {
        var builder = Jwts.builder().subject(EMAIL_A)
                .issuedAt(new Date()).expiration(Date.from(Instant.now().plusSeconds(300)));
        String token = switch (algorithm) {
            case "HS256" -> builder.signWith(KEY, Jwts.SIG.HS256).compact();
            case "HS384" -> builder.signWith(KEY, Jwts.SIG.HS384).compact();
            default -> builder.signWith(KEY, Jwts.SIG.HS512).compact();
        };
        when(repository.findByEmailDestinatarioOrderByFechaEnvioDesc(EMAIL_A)).thenReturn(List.of());

        mvc.perform(get("/api/notificaciones").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void assertRejected(String token) throws Exception {
        mvc.perform(get("/api/notificaciones")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Email", EMAIL_B))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/notificaciones/leer-todas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }

    private String bearer(String email) {
        return "Bearer " + token(email, Instant.now().plusSeconds(300));
    }

    private String token(String email, Instant expiration) {
        return Jwts.builder().subject(email).issuedAt(new Date())
                .expiration(Date.from(expiration)).signWith(KEY).compact();
    }

    private HistorialNotificacion notificacion(Long id, String email) {
        return new HistorialNotificacion(id, email, TipoNotificacion.ALERTA_INTERNA,
                EstadoNotificacion.ENVIADO, LocalDateTime.of(2026, 11, 5, 12, 0),
                "Diagnostico interno que no debe salir en la API", "Aviso", "Mensaje", false, true);
    }
}
