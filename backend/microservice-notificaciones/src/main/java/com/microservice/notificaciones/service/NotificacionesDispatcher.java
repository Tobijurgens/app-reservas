package com.microservice.notificaciones.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@ConditionalOnProperty(name = "app.notificaciones.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class NotificacionesDispatcher {
    private final ProcesadorNotificacion procesador;
    private final int lote;

    public NotificacionesDispatcher(ProcesadorNotificacion procesador,
            @Value("${app.notificaciones.batch-size:10}") int lote) {
        Assert.isTrue(lote > 0 && lote <= 100, "El lote de notificaciones debe estar entre 1 y 100.");
        this.procesador = procesador; this.lote = lote;
    }

    @Scheduled(fixedDelayString = "${app.notificaciones.poll-ms:3000}", initialDelayString = "${app.notificaciones.poll-ms:3000}")
    public void procesarPendientes() {
        try {
            for (int i = 0; i < lote && !Thread.currentThread().isInterrupted(); i++)
                if (!procesador.procesarSiguiente()) break;
        } catch (RuntimeException ex) {
            log.error("Notificaciones ciclo pendiente error={}", ex.getClass().getSimpleName());
        }
    }
}
