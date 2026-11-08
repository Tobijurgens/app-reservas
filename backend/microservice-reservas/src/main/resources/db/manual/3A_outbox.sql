-- Migración ADITIVA manual. No requiere Flyway y no borra información.
-- Aplicar en la base de Reservas antes de desplegar si no se usa ddl-auto=update.
CREATE TABLE IF NOT EXISTS eventos_outbox (
    id uuid PRIMARY KEY,
    exchange varchar(100) NOT NULL,
    routing_key varchar(100) NOT NULL,
    tipo_evento varchar(255) NOT NULL,
    payload text NOT NULL,
    creado_en timestamp with time zone NOT NULL,
    proximo_intento timestamp with time zone NOT NULL,
    intentos integer NOT NULL DEFAULT 0,
    ultimo_error varchar(100),
    publicado_en timestamp with time zone
);

CREATE INDEX IF NOT EXISTS idx_outbox_pendientes
    ON eventos_outbox (publicado_en, proximo_intento, creado_en);
