package com.microservice.reservas.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class OutboxDispatcher {
    private final OutboxPublisher publisher;
    private final int batchSize;

    public OutboxDispatcher(OutboxPublisher publisher, @Value("${app.outbox.batch-size:10}") int batchSize) {
        Assert.isTrue(batchSize > 0 && batchSize <= 100, "El lote del outbox debe estar entre 1 y 100.");
        this.publisher = publisher;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-ms:3000}", initialDelayString = "${app.outbox.poll-ms:3000}")
    public void publicarPendientes() {
        try {
            for (int i = 0; i < batchSize && !Thread.currentThread().isInterrupted(); i++) {
                if (!publisher.publicarSiguiente()) break;
            }
        } catch (RuntimeException ex) {
            // Un error de DB/commit deja el evento pendiente; nunca se propaga a una operación de negocio.
            log.error("Outbox ciclo interrumpido error={}", ex.getClass().getSimpleName());
        }
    }
}
