package com.microservice.reservas.service;

import com.microservice.reservas.entity.EventoOutbox;
import com.microservice.reservas.repository.EventoOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {
    private final EventoOutboxRepository repository;
    private final MessageConverter converter;

    @Transactional(propagation = Propagation.MANDATORY)
    public void encolar(String exchange, String routingKey, Object payload) {
        EventoOutbox evento = new EventoOutbox();
        Message mensaje;
        try {
            mensaje = converter.toMessage(payload, new MessageProperties());
        } catch (RuntimeException ex) {
            // No registrar el objeto, el mensaje de la excepción ni su stack: pueden contener tokens.
            log.error("Outbox serializacion fallida id={} ruta={} tipo={} error={}", evento.getId(), routingKey,
                    payload == null ? "null" : payload.getClass().getName(), ex.getClass().getSimpleName());
            throw new IllegalStateException("No se pudo registrar el evento de la operación.");
        }
        evento.setExchange(exchange);
        evento.setRoutingKey(routingKey);
        evento.setTipoEvento(payload.getClass().getName());
        evento.setPayload(new String(mensaje.getBody(), StandardCharsets.UTF_8));
        repository.save(evento);
        log.debug("Outbox evento pendiente id={} ruta={}", evento.getId(), routingKey);
    }
}
