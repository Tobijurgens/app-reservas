package com.microservice.notificaciones.entity;

import com.microservice.notificaciones.enums.EstadoProcesamiento;
import com.microservice.notificaciones.enums.TipoEventoRecibido;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "eventos_recibidos", indexes = @Index(name = "idx_recibidos_pendientes",
        columnList = "estado,proximo_intento,creado_en"))
@Getter
@Setter
@NoArgsConstructor
public class EventoRecibido {
    @Id
    @Column(name = "event_id", length = 100)
    private String eventId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEventoRecibido tipo;
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] payload;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoProcesamiento estado;
    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;
    @Column(name = "proximo_intento")
    private Instant proximoIntento;
    @Column(nullable = false)
    private int intentos;
    @Column(name = "ultimo_error", length = 100)
    private String ultimoError;
    @Column(name = "historial_id")
    private Long historialId;
    @Column(name = "finalizado_en")
    private Instant finalizadoEn;
}
