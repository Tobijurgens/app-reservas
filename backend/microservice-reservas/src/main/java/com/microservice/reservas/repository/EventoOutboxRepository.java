package com.microservice.reservas.repository;

import com.microservice.reservas.entity.EventoOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface EventoOutboxRepository extends JpaRepository<EventoOutbox, UUID> {
    @Query(value = "SELECT * FROM eventos_outbox WHERE publicado_en IS NULL "
            + "AND proximo_intento <= CURRENT_TIMESTAMP ORDER BY creado_en, id LIMIT 1 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    Optional<EventoOutbox> siguientePendiente();
}
