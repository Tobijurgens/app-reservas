package com.microservice.notificaciones.service;

import com.microservice.notificaciones.dto.*;
import com.microservice.notificaciones.entity.EventoRecibido;
import com.microservice.notificaciones.entity.HistorialNotificacion;
import com.microservice.notificaciones.enums.EstadoNotificacion;
import com.microservice.notificaciones.enums.EstadoProcesamiento;
import com.microservice.notificaciones.enums.TipoNotificacion;
import com.microservice.notificaciones.repository.EventoRecibidoRepository;
import com.microservice.notificaciones.repository.HistorialNotificacionRepository;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@Slf4j
public class ProcesadorNotificacion {
    private final EventoRecibidoRepository eventos;
    private final HistorialNotificacionRepository historiales;
    private final EmailService emailService;
    private final JsonMapper json = JsonMapper.builder()
            .enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private final int maxIntentos;
    private final long retryInitialMs;
    private final long retryMaxMs;

    public ProcesadorNotificacion(EventoRecibidoRepository eventos, HistorialNotificacionRepository historiales,
            EmailService emailService, @Value("${app.notificaciones.max-intentos:5}") int maxIntentos,
            @Value("${app.notificaciones.retry-initial-ms:60000}") long retryInitialMs,
            @Value("${app.notificaciones.retry-max-ms:300000}") long retryMaxMs) {
        Assert.isTrue(maxIntentos > 0 && retryInitialMs > 0 && retryMaxMs >= retryInitialMs, "Política SMTP inválida.");
        this.eventos = eventos; this.historiales = historiales; this.emailService = emailService;
        this.maxIntentos = maxIntentos; this.retryInitialMs = retryInitialMs; this.retryMaxMs = retryMaxMs;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean procesarSiguiente() {
        var pendiente = eventos.siguientePendiente();
        if (pendiente.isEmpty()) return false;
        EventoRecibido evento = pendiente.get();
        HistorialNotificacion historial = evento.getHistorialId() == null ? null
                : historiales.findById(evento.getHistorialId()).orElse(null);
        if (evento.getIntentos() >= maxIntentos) {
            registrarFallo(evento, historial, new FalloEnvio(true, "LIMITE_INTENTOS"));
            return true;
        }
        evento.setIntentos(evento.getIntentos() + 1);
        Preparacion preparacion;
        try {
            preparacion = preparar(evento);
        } catch (RuntimeException | AddressException ex) {
            registrarFallo(evento, historial, FalloEnvio.clasificar(ex));
            return true;
        }
        if (preparacion.eliminar()) {
            historiales.deleteAllByEmailDestinatario(preparacion.email());
            completar(evento);
            return true;
        }
        if (historial == null) historial = new HistorialNotificacion();
        historial.setEmailDestinatario(preparacion.email());
        historial.setTipo(preparacion.tipo());
        historial.setTitulo(preparacion.titulo());
        historial.setMensaje(preparacion.mensaje());
        historial.setSoloInApp(preparacion.soloInApp());
        historial.setFechaEnvio(LocalDateTime.now());
        // Se valida y persiste la estructura antes de SMTP, sin anticipar ENVIADO ni hacer commit.
        historial.setEstado(EstadoNotificacion.ERROR);
        historial.setDetalleError("ENVIO_PENDIENTE");
        historiales.saveAndFlush(historial);
        evento.setHistorialId(historial.getId());
        try {
            preparacion.enviar().run();
            historial.setEstado(EstadoNotificacion.ENVIADO);
            historial.setDetalleError(null);
            completar(evento);
        } catch (RuntimeException ex) {
            registrarFallo(evento, historial, FalloEnvio.clasificar(ex));
        }
        return true;
    }

    private void completar(EventoRecibido evento) {
        evento.setEstado(EstadoProcesamiento.COMPLETADO);
        evento.setFinalizadoEn(Instant.now());
        evento.setProximoIntento(null);
        evento.setUltimoError(null);
        log.info("Notificacion completada id={} tipo={} intento={}", evento.getEventId(), evento.getTipo(), evento.getIntentos());
    }

    private void registrarFallo(EventoRecibido evento, HistorialNotificacion historial, FalloEnvio fallo) {
        evento.setUltimoError(fallo.codigo());
        if (historial != null) {
            historial.setEstado(EstadoNotificacion.ERROR);
            historial.setDetalleError(fallo.codigo());
            historial.setFechaEnvio(LocalDateTime.now());
        }
        if (fallo.permanente() || evento.getIntentos() >= maxIntentos) {
            evento.setEstado(EstadoProcesamiento.FALLIDO);
            evento.setProximoIntento(null);
            evento.setFinalizadoEn(Instant.now());
            log.warn("Notificacion fallo definitivo id={} intento={} error={}", evento.getEventId(), evento.getIntentos(), fallo.codigo());
        } else {
            long espera = retryInitialMs;
            for (int i = 1; i < evento.getIntentos() && espera < retryMaxMs; i++)
                espera = espera > retryMaxMs / 2 ? retryMaxMs : Math.min(retryMaxMs, espera * 2);
            evento.setProximoIntento(Instant.now().plusMillis(espera));
            log.warn("Notificacion reintento id={} intento={} error={} proximo={}", evento.getEventId(),
                    evento.getIntentos(), fallo.codigo(), evento.getProximoIntento());
        }
    }

    private Preparacion preparar(EventoRecibido evento) throws AddressException {
        return switch (evento.getTipo()) {
            case REGISTRO -> {
                var e = leer(evento, UsuarioRegistradoEvent.class);
                validarEmail(e.getEmail()); validarTexto(e.getToken());
                yield new Preparacion(e.getEmail(), TipoNotificacion.VERIFICACION_CUENTA, "¡Bienvenido a AgendApp!",
                        "¡Qué bueno tenerte acá! Ya podés empezar a gestionar tus turnos y organizar tu agenda.", false, false,
                        () -> emailService.enviarCorreoVerificacion(e.getEmail(), e.getNombre(), e.getToken()));
            }
            case INVITACION -> {
                var e = leer(evento, InvitacionEmpleadoEvent.class);
                validarEmail(e.getEmailDestino()); validarTexto(e.getToken());
                yield new Preparacion(e.getEmailDestino(), TipoNotificacion.INVITACION_EMPLEADO, "Nueva Invitación de Staff",
                        e.getNombreNegocio() + " te invitó a unirte como " + e.getNombreRecurso() + ". Revisa la bandeja de entrada de tu correo", false, false,
                        () -> emailService.enviarCorreoInvitacion(e.getEmailDestino(), e.getNombreNegocio(), e.getNombreRecurso(), e.getToken()));
            }
            case TURNO -> {
                var e = leer(evento, TurnoEvent.class);
                validarEmail(e.getEmailDestino());
                if (!"CONFIRMACION".equals(e.getTipoEvento()) && !"CANCELACION".equals(e.getTipoEvento()))
                    throw new IllegalArgumentException("Tipo de turno inválido");
                boolean confirmado = "CONFIRMACION".equals(e.getTipoEvento());
                yield new Preparacion(e.getEmailDestino(), TipoNotificacion.TURNO_CLIENTE,
                        confirmado ? "Turno Confirmado" : "Turno Cancelado",
                        "Tu turno en " + e.getNombreNegocio() + " ha sido " + (confirmado ? "confirmado" : "cancelado") + ".",
                        false, false, () -> emailService.enviarCorreoTurno(e));
            }
            case AVISO -> {
                var e = leer(evento, AvisoInternoEvent.class);
                validarEmail(e.getEmailDestino()); validarTexto(e.getTitulo()); validarTexto(e.getMensaje());
                yield new Preparacion(e.getEmailDestino(), TipoNotificacion.ALERTA_INTERNA,
                        e.getTitulo(), e.getMensaje(), true, false, () -> {});
            }
            case ELIMINACION -> {
                var e = leer(evento, UsuarioEliminadoEvent.class);
                validarEmail(e.getEmail());
                yield new Preparacion(e.getEmail(), null, null, null, true, true, () -> {});
            }
        };
    }

    private <T> T leer(EventoRecibido evento, Class<T> tipo) {
        T valor = json.readValue(evento.getPayload(), tipo);
        if (valor == null) throw new IllegalArgumentException("Evento vacío");
        return valor;
    }

    private void validarEmail(String email) throws AddressException {
        if (email == null || email.length() > 254 || !email.contains("@")) throw new AddressException("Destinatario inválido");
        new InternetAddress(email, true).validate();
    }

    private void validarTexto(String texto) {
        if (texto == null || texto.isBlank()) throw new IllegalArgumentException("Falta un dato obligatorio del evento");
    }

    private record Preparacion(String email, TipoNotificacion tipo, String titulo, String mensaje,
            boolean soloInApp, boolean eliminar, Runnable enviar) {}
}
