package com.microservice.reservas.service;

import com.microservice.reservas.entity.EventoOutbox;
import com.microservice.reservas.repository.EventoOutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class OutboxPublisher {
    private final EventoOutboxRepository repository;
    private final RabbitTemplate rabbit;
    private final long confirmTimeoutMs;
    private final long retryInitialMs;
    private final long retryMaxMs;

    public OutboxPublisher(EventoOutboxRepository repository, RabbitTemplate rabbit,
            @Value("${app.outbox.confirm-timeout-ms:5000}") long confirmTimeoutMs,
            @Value("${app.outbox.retry-initial-ms:60000}") long retryInitialMs,
            @Value("${app.outbox.retry-max-ms:300000}") long retryMaxMs) {
        Assert.isTrue(confirmTimeoutMs > 0 && retryInitialMs > 0 && retryMaxMs >= retryInitialMs,
                "Los tiempos del outbox deben ser positivos y el máximo debe ser mayor o igual al inicial.");
        this.repository = repository;
        this.rabbit = rabbit;
        this.confirmTimeoutMs = confirmTimeoutMs;
        this.retryInitialMs = retryInitialMs;
        this.retryMaxMs = retryMaxMs;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean publicarSiguiente() {
        var pendiente = repository.siguientePendiente();
        if (pendiente.isEmpty()) return false;
        EventoOutbox evento = pendiente.get();
        evento.setIntentos(evento.getIntentos() + 1);
        String error = publicar(evento);
        if (error == null) {
            evento.setPublicadoEn(Instant.now());
            evento.setUltimoError(null);
            log.info("Outbox confirmado id={} ruta={} intento={}", evento.getId(), evento.getRoutingKey(), evento.getIntentos());
        } else {
            evento.setUltimoError(error);
            long espera = retryInitialMs;
            for (int i = 1; i < evento.getIntentos() && espera < retryMaxMs; i++) {
                espera = espera > retryMaxMs / 2 ? retryMaxMs : Math.min(retryMaxMs, espera * 2);
            }
            evento.setProximoIntento(Instant.now().plusMillis(espera));
            log.warn("Outbox pendiente id={} ruta={} intento={} error={} proximo={}", evento.getId(),
                    evento.getRoutingKey(), evento.getIntentos(), error, evento.getProximoIntento());
        }
        repository.save(evento);
        return true;
    }

    private String publicar(EventoOutbox evento) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(evento.getId().toString());
        properties.setHeader("eventId", evento.getId().toString());
        properties.setHeader("__TypeId__", evento.getTipoEvento());
        Message message = new Message(evento.getPayload().getBytes(StandardCharsets.UTF_8), properties);
        // La correlación debe ser única por INTENTO; messageId permanece estable para el evento.
        CorrelationData correlation = new CorrelationData();
        try {
            rabbit.send(evento.getExchange(), evento.getRoutingKey(), message, correlation);
            var confirm = correlation.getFuture().get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
            if (!confirm.ack()) return "NACK";
            // Spring AMQP completa el return antes de completar el future del confirm.
            if (correlation.getReturned() != null) return "SIN_RUTA";
            return null;
        } catch (TimeoutException ex) {
            return "TIMEOUT_CONFIRMACION";
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "INTERRUMPIDO";
        } catch (ExecutionException | RuntimeException ex) {
            return "PUBLICACION_FALLIDA";
        }
    }
}
