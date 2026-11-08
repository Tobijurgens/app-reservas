package com.microservice.reservas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "eventos_outbox", indexes = @Index(name = "idx_outbox_pendientes",
        columnList = "publicado_en,proximo_intento,creado_en"))
@Getter
@Setter
@NoArgsConstructor
public class EventoOutbox {
    @Id
    private UUID id = UUID.randomUUID();
    @Column(nullable = false, length = 100)
    private String exchange;
    @Column(name = "routing_key", nullable = false, length = 100)
    private String routingKey;
    @Column(name = "tipo_evento", nullable = false, length = 255)
    private String tipoEvento;
    @Column(nullable = false, columnDefinition = "text")
    private String payload;
    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();
    @Column(name = "proximo_intento", nullable = false)
    private Instant proximoIntento = creadoEn;
    @Column(nullable = false)
    private int intentos;
    @Column(name = "ultimo_error", length = 100)
    private String ultimoError;
    @Column(name = "publicado_en")
    private Instant publicadoEn;
}
