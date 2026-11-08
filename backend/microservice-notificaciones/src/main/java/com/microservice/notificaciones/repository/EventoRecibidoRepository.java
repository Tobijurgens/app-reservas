package com.microservice.notificaciones.repository;

import com.microservice.notificaciones.entity.EventoRecibido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventoRecibidoRepository extends JpaRepository<EventoRecibido, String> {
    @Modifying
    @Query(value = "INSERT INTO eventos_recibidos (event_id, tipo, payload, estado, creado_en, proximo_intento, intentos) "
            + "VALUES (:id, :tipo, :payload, 'PENDIENTE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0) "
            + "ON CONFLICT (event_id) DO NOTHING", nativeQuery = true)
    int insertarSiNoExiste(@Param("id") String id, @Param("tipo") String tipo, @Param("payload") byte[] payload);

    @Query(value = "SELECT * FROM eventos_recibidos WHERE estado = 'PENDIENTE' AND proximo_intento <= CURRENT_TIMESTAMP "
            + "ORDER BY creado_en, event_id LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<EventoRecibido> siguientePendiente();
}
