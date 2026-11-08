package com.microservice.notificaciones.service;

import com.microservice.notificaciones.enums.TipoEventoRecibido;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IdentidadEventoTest {
    @Test
    void usaEventIdEstableYConservaMessageIdDe3AComoFallback() {
        String id = UUID.randomUUID().toString();
        var props = new MessageProperties(); props.setMessageId(id);
        var mensaje = new Message(new byte[0], props);
        assertEquals(id, IdentidadEvento.obtener(mensaje, TipoEventoRecibido.REGISTRO));
        props.setHeader("eventId", " ");
        assertEquals(id, IdentidadEvento.obtener(mensaje, TipoEventoRecibido.REGISTRO));
        String header = UUID.randomUUID().toString(); props.setHeader("eventId", header);
        assertEquals(header, IdentidadEvento.obtener(mensaje, TipoEventoRecibido.REGISTRO));
    }

    @Test
    void legacySinIdTieneHuellaEstableDeTipoYCuerpoInclusoSiEsMalformado() {
        byte[] cuerpo = {0, 1, 2};
        Message m = new Message(cuerpo, new MessageProperties());
        String id = IdentidadEvento.obtener(m, TipoEventoRecibido.REGISTRO);
        assertTrue(id.startsWith("legacy:"));
        assertEquals(id, IdentidadEvento.obtener(new Message(cuerpo.clone(), new MessageProperties()), TipoEventoRecibido.REGISTRO));
        assertNotEquals(id, IdentidadEvento.obtener(m, TipoEventoRecibido.INVITACION));
    }

    @Test
    void identificadoresExternosNoExponenSuContenidoEnLogs() {
        var props = new MessageProperties(); props.setHeader("eventId", "https://secreto?token=secreto");
        Message m = new Message("{}".getBytes(StandardCharsets.UTF_8), props);
        String id = IdentidadEvento.obtener(m, TipoEventoRecibido.REGISTRO);
        assertTrue(id.startsWith("externo:"));
        assertFalse(id.contains("secreto"));
        assertEquals(id, IdentidadEvento.obtener(m, TipoEventoRecibido.REGISTRO));
    }

    @Test
    void headerBinarioNoUsaLaIdentidadAleatoriaDelObjetoJava() {
        var primera = new MessageProperties(); primera.setHeader("eventId", new byte[] {1, 2, 3});
        var segunda = new MessageProperties(); segunda.setHeader("eventId", new byte[] {1, 2, 3});
        assertEquals(IdentidadEvento.obtener(new Message(new byte[0], primera), TipoEventoRecibido.REGISTRO),
                IdentidadEvento.obtener(new Message(new byte[0], segunda), TipoEventoRecibido.REGISTRO));
        String id = UUID.randomUUID().toString(); segunda.setMessageId(id); segunda.setHeader("eventId", new Object());
        assertEquals(id, IdentidadEvento.obtener(new Message(new byte[0], segunda), TipoEventoRecibido.REGISTRO));
    }
}
