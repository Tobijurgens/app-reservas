package com.microservice.reservas.service;

import com.microservice.reservas.dto.event.InvitacionEmpleadoEvent;
import com.microservice.reservas.entity.EventoOutbox;
import com.microservice.reservas.repository.EventoOutboxRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxServiceTest {
    @Test
    void guardaElJsonOriginalSinGenerarOtroToken() {
        var repo = mock(EventoOutboxRepository.class);
        var converter = new JacksonJsonMessageConverter();
        var service = new OutboxService(repo, converter);
        String token = UUID.randomUUID().toString();
        var dto = new InvitacionEmpleadoEvent("empleado@example.com", "Local", "Agenda", token);
        service.encolar("notificaciones.exchange", "invitacion.email.routing.key", dto);
        var captor = ArgumentCaptor.forClass(EventoOutbox.class);
        verify(repo).save(captor.capture());
        var evento = captor.getValue();
        assertEquals(new String(converter.toMessage(dto, new org.springframework.amqp.core.MessageProperties()).getBody(),
                StandardCharsets.UTF_8), evento.getPayload());
        assertEquals(dto.getClass().getName(), evento.getTipoEvento());
        assertNotNull(evento.getId());
        assertNull(evento.getPublicadoEn());
        assertEquals(0, evento.getIntentos());
    }

    @Test
    void falloDeSerializacionEsExplicitoSinExponerElPayload() {
        var repo = mock(EventoOutboxRepository.class);
        var converter = mock(MessageConverter.class);
        when(converter.toMessage(any(), any())).thenThrow(new MessageConversionException("token-que-no-debe-salir"));
        var service = new OutboxService(repo, converter);
        var error = assertThrows(IllegalStateException.class, () -> service.encolar("exchange", "ruta", new Object()));
        assertEquals("No se pudo registrar el evento de la operación.", error.getMessage());
        assertNull(error.getCause());
        verifyNoInteractions(repo);
    }
}
