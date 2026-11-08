package com.microservice.notificaciones.service;

import com.microservice.notificaciones.enums.TipoEventoRecibido;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public final class IdentidadEvento {
    private IdentidadEvento() {}

    public static String obtener(Message mensaje, TipoEventoRecibido tipo) {
        Object header = mensaje.getMessageProperties().getHeaders().get("eventId");
        if (header instanceof byte[] bytes) return "externo:" + hash(bytes);
        String id = header instanceof CharSequence texto ? texto.toString()
                : header instanceof com.rabbitmq.client.LongString texto ? texto.toString() : null;
        if (id == null || id.isBlank()) id = mensaje.getMessageProperties().getMessageId();
        if (id != null && !id.isBlank()) {
            try {
                if (id.length() == 36) return UUID.fromString(id).toString();
            } catch (IllegalArgumentException ignored) { /* ID externo: no registrarlo sin sanitizar. */ }
            return "externo:" + hash(id.getBytes(StandardCharsets.UTF_8));
        }
        byte[] prefijo = (tipo.name() + ":").getBytes(StandardCharsets.UTF_8);
        byte[] contenido = new byte[prefijo.length + mensaje.getBody().length];
        System.arraycopy(prefijo, 0, contenido, 0, prefijo.length);
        System.arraycopy(mensaje.getBody(), 0, contenido, prefijo.length, mensaje.getBody().length);
        return "legacy:" + hash(contenido);
    }

    private static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 no disponible"); }
    }
}
