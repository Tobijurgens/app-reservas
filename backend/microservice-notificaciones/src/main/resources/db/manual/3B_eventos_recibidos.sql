-- Migración aditiva e idempotente para la base de NOTIFICACIONES exclusivamente.
CREATE TABLE IF NOT EXISTS eventos_recibidos (
    event_id varchar(100) PRIMARY KEY,
    tipo varchar(30) NOT NULL,
    payload bytea NOT NULL,
    estado varchar(30) NOT NULL,
    creado_en timestamp with time zone NOT NULL,
    proximo_intento timestamp with time zone,
    intentos integer NOT NULL DEFAULT 0,
    ultimo_error varchar(100),
    historial_id bigint,
    finalizado_en timestamp with time zone
);
CREATE INDEX IF NOT EXISTS idx_recibidos_pendientes ON eventos_recibidos (estado, proximo_intento, creado_en);

-- Ampliación sin truncar registros. Aplicar explícitamente: Hibernate puede considerar
-- varchar/text equivalentes y no ampliar estas columnas existentes con ddl-auto=update.
ALTER TABLE IF EXISTS historial_notificaciones ALTER COLUMN titulo TYPE text;
ALTER TABLE IF EXISTS historial_notificaciones ALTER COLUMN mensaje TYPE text;
