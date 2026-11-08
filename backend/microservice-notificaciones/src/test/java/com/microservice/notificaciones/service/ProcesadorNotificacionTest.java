package com.microservice.notificaciones.service;

import com.microservice.notificaciones.dto.*;
import com.microservice.notificaciones.entity.*;
import com.microservice.notificaciones.enums.*;
import com.microservice.notificaciones.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mail.MailSendException;
import org.thymeleaf.exceptions.TemplateProcessingException;
import tools.jackson.databind.json.JsonMapper;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProcesadorNotificacionTest {
    private EventoRecibidoRepository eventos;
    private HistorialNotificacionRepository historiales;
    private EmailService email;
    private ProcesadorNotificacion procesador;
    private final Map<Long, HistorialNotificacion> historialGuardado = new HashMap<>();

    @BeforeEach
    void preparar() {
        eventos = mock(EventoRecibidoRepository.class);
        historiales = mock(HistorialNotificacionRepository.class);
        email = mock(EmailService.class);
        procesador = new ProcesadorNotificacion(eventos, historiales, email, 5, 60000, 300000);
        when(historiales.saveAndFlush(any())).thenAnswer(c -> {
            HistorialNotificacion h = c.getArgument(0);
            if (h.getId() == null) h.setId((long) historialGuardado.size() + 1);
            historialGuardado.put(h.getId(), h);
            return h;
        });
        when(historiales.findById(anyLong())).thenAnswer(c -> Optional.ofNullable(historialGuardado.get(c.getArgument(0))));
    }

    @ParameterizedTest
    @EnumSource(TipoEventoRecibido.class)
    void conservaTodosLosFlujosYMarcaExitoDespuesDelEfecto(TipoEventoRecibido tipo) {
        EventoRecibido e = evento(tipo);
        when(eventos.siguientePendiente()).thenReturn(Optional.of(e));
        doAnswer(c -> {
            assertEquals(EstadoProcesamiento.PENDIENTE, e.getEstado());
            assertEquals(EstadoNotificacion.ERROR, historialGuardado.values().iterator().next().getEstado());
            return null;
        }).when(email).enviarCorreoVerificacion(anyString(), anyString(), anyString());
        procesador.procesarSiguiente();
        assertEquals(EstadoProcesamiento.COMPLETADO, e.getEstado());
        assertNull(e.getProximoIntento());
        switch (tipo) {
            case REGISTRO -> verify(email).enviarCorreoVerificacion("a@example.com", "Ana", "token-de-prueba");
            case INVITACION -> verify(email).enviarCorreoInvitacion("a@example.com", "Local", "Agenda", "token-de-prueba");
            case TURNO -> verify(email).enviarCorreoTurno(any(TurnoEvent.class));
            case AVISO -> { verifyNoInteractions(email); assertTrue(historialGuardado.values().iterator().next().isSoloInApp()); }
            case ELIMINACION -> { verifyNoInteractions(email); verify(historiales).deleteAllByEmailDestinatario("a@example.com"); }
        }
        if (tipo != TipoEventoRecibido.ELIMINACION) assertEquals(EstadoNotificacion.ENVIADO,
                historialGuardado.values().iterator().next().getEstado());
    }

    @Test
    void smtpDesconectadoProgramaReintentoYRecuperaSinDuplicarHistorial() {
        EventoRecibido e = evento(TipoEventoRecibido.INVITACION);
        when(eventos.siguientePendiente()).thenReturn(Optional.of(e));
        doThrow(new MailSendException("detalle sensible", new ConnectException())).doNothing()
                .when(email).enviarCorreoInvitacion(anyString(), anyString(), anyString(), anyString());
        Instant antes = Instant.now();
        procesador.procesarSiguiente();
        assertEquals(EstadoProcesamiento.PENDIENTE, e.getEstado());
        assertEquals("SMTP_CONEXION", e.getUltimoError());
        assertFalse(e.getProximoIntento().isBefore(antes.plusSeconds(60)));
        assertFalse(e.getProximoIntento().isAfter(Instant.now().plusSeconds(60)));
        procesador.procesarSiguiente(); // El reloj/selección SQL se comprueba en PostgreSQL, no en este mock.
        assertEquals(EstadoProcesamiento.COMPLETADO, e.getEstado());
        assertEquals(2, e.getIntentos());
        assertEquals(1, historialGuardado.size());
        verify(email, times(2)).enviarCorreoInvitacion("a@example.com", "Local", "Agenda", "token-de-prueba");
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONFIRMACION", "CANCELACION"})
    void conservaCorreosDeConfirmacionYCancelacion(String tipo) {
        EventoRecibido e = evento(TipoEventoRecibido.TURNO);
        e.setPayload(JsonMapper.builder().build().writeValueAsBytes(new TurnoEvent("a@example.com", "Ana", "Local",
                "Direccion", "Agenda", "Servicio", "07/11/2026 12:00", tipo)));
        when(eventos.siguientePendiente()).thenReturn(Optional.of(e));
        procesador.procesarSiguiente();
        verify(email).enviarCorreoTurno(argThat(t -> t.getTipoEvento().equals(tipo)));
        assertEquals(EstadoProcesamiento.COMPLETADO, e.getEstado());
        assertEquals(tipo.equals("CONFIRMACION") ? "Turno Confirmado" : "Turno Cancelado",
                historialGuardado.values().iterator().next().getTitulo());
    }

    @Test
    void timeoutEsTemporalYElLimiteDeCincoIntentosConservaEvidencia() {
        EventoRecibido e = evento(TipoEventoRecibido.REGISTRO);
        when(eventos.siguientePendiente()).thenReturn(Optional.of(e));
        doThrow(new MailSendException("dato sensible", new SocketTimeoutException()))
                .when(email).enviarCorreoVerificacion(anyString(), anyString(), anyString());
        long[] esperas = {60, 120, 240, 300};
        for (int i = 0; i < 5; i++) {
            Instant antes = Instant.now();
            procesador.procesarSiguiente();
            if (i < 4) {
                assertEquals(EstadoProcesamiento.PENDIENTE, e.getEstado());
                assertFalse(e.getProximoIntento().isBefore(antes.plusSeconds(esperas[i])));
                assertFalse(e.getProximoIntento().isAfter(Instant.now().plusSeconds(esperas[i])));
            }
        }
        assertEquals(EstadoProcesamiento.FALLIDO, e.getEstado());
        assertEquals("SMTP_TIMEOUT", e.getUltimoError());
        assertNull(e.getProximoIntento());
        assertNotNull(e.getFinalizadoEn());
        assertEquals(EstadoNotificacion.ERROR, historialGuardado.values().iterator().next().getEstado());
        verify(email, times(5)).enviarCorreoVerificacion(anyString(), anyString(), anyString());
    }

    @Test
    void falloDePlantillaEsDefinitivoSinExponerSuMensaje() {
        EventoRecibido e = evento(TipoEventoRecibido.REGISTRO);
        when(eventos.siguientePendiente()).thenReturn(Optional.of(e));
        doThrow(new TemplateProcessingException("enlace secreto"))
                .when(email).enviarCorreoVerificacion(anyString(), anyString(), anyString());
        procesador.procesarSiguiente();
        assertEquals(EstadoProcesamiento.FALLIDO, e.getEstado());
        assertEquals("PLANTILLA_INVALIDA", e.getUltimoError());
        assertEquals("PLANTILLA_INVALIDA", historialGuardado.values().iterator().next().getDetalleError());
    }

    @Test
    void bajarElLimiteNoPermiteOtroEnvioAUnPendienteQueYaLoAlcanzo() {
        EventoRecibido e = evento(TipoEventoRecibido.REGISTRO);
        e.setIntentos(3);
        when(eventos.siguientePendiente()).thenReturn(Optional.of(e));
        new ProcesadorNotificacion(eventos, historiales, email, 3, 60000, 300000).procesarSiguiente();
        assertEquals(EstadoProcesamiento.FALLIDO, e.getEstado());
        assertEquals(3, e.getIntentos());
        assertEquals("LIMITE_INTENTOS", e.getUltimoError());
        verifyNoInteractions(email);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "null", "{}", "[]", "{\"email\":\"invalido\",\"token\":\"secreto\"}", "{} basura"})
    void mensajesInvalidosQuedanComoFalloDefinitivoSinSmtp(String payload) {
        EventoRecibido e = evento(TipoEventoRecibido.REGISTRO);
        e.setPayload(payload.getBytes(StandardCharsets.UTF_8));
        when(eventos.siguientePendiente()).thenReturn(Optional.of(e));
        procesador.procesarSiguiente();
        assertEquals(EstadoProcesamiento.FALLIDO, e.getEstado());
        assertNull(e.getProximoIntento());
        verifyNoInteractions(email);
    }

    @Test
    void unaNotificacionFallidaNoImpideProcesarLaSiguiente() {
        EventoRecibido mala = evento(TipoEventoRecibido.REGISTRO);
        EventoRecibido buena = evento(TipoEventoRecibido.AVISO);
        when(eventos.siguientePendiente()).thenReturn(Optional.of(mala), Optional.of(buena), Optional.empty());
        doThrow(new MailSendException("caída temporal")).when(email).enviarCorreoVerificacion(anyString(), anyString(), anyString());
        new NotificacionesDispatcher(procesador, 10).procesarPendientes();
        assertEquals(EstadoProcesamiento.PENDIENTE, mala.getEstado());
        assertEquals(EstadoProcesamiento.COMPLETADO, buena.getEstado());
    }

    private EventoRecibido evento(TipoEventoRecibido tipo) {
        Object dto = switch (tipo) {
            case REGISTRO -> new UsuarioRegistradoEvent("a@example.com", "Ana", "token-de-prueba");
            case INVITACION -> new InvitacionEmpleadoEvent("a@example.com", "Local", "Agenda", "token-de-prueba");
            case TURNO -> new TurnoEvent("a@example.com", "Ana", "Local", "Dirección", "Agenda", "Servicio", "07/11/2026 12:00", "CONFIRMACION");
            case AVISO -> new AvisoInternoEvent("a@example.com", "Aviso", "Mensaje");
            case ELIMINACION -> new UsuarioEliminadoEvent("a@example.com");
        };
        EventoRecibido e = new EventoRecibido();
        e.setEventId(UUID.randomUUID().toString()); e.setTipo(tipo); e.setEstado(EstadoProcesamiento.PENDIENTE);
        e.setCreadoEn(Instant.now()); e.setProximoIntento(Instant.now());
        e.setPayload(JsonMapper.builder().build().writeValueAsBytes(dto));
        return e;
    }
}
