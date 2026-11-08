package com.microservice.reservas.service;

import com.microservice.reservas.entity.EventoOutbox;
import com.microservice.reservas.repository.EventoOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Los ACK de esta clase son simulados. La prueba PostgreSQL/Rabbit comprueba el broker real. */
class OutboxPublisherTest {
    private EventoOutboxRepository repository;
    private RabbitTemplate rabbit;
    private OutboxPublisher publisher;
    private EventoOutbox evento;

    @BeforeEach
    void preparar() {
        repository = mock(EventoOutboxRepository.class);
        rabbit = mock(RabbitTemplate.class);
        publisher = new OutboxPublisher(repository, rabbit, 50, 10, 80);
        evento = new EventoOutbox();
        evento.setExchange("notificaciones.exchange");
        evento.setRoutingKey("turno.cliente.routing.key");
        evento.setTipoEvento("com.microservice.reservas.dto.event.TurnoEvent");
        evento.setPayload("{\"tipoEvento\":\"CONFIRMACION\"}");
        when(repository.siguientePendiente()).thenReturn(Optional.of(evento));
    }

    @Test
    void ackSinReturnMarcaPublicadoYConservaContratoPersistente() {
        doAnswer(c -> {
            Message msg = c.getArgument(2);
            assertEquals(evento.getId().toString(), msg.getMessageProperties().getMessageId());
            assertEquals(evento.getId().toString(), msg.getMessageProperties().getHeader("eventId"));
            assertEquals(MessageDeliveryMode.PERSISTENT, msg.getMessageProperties().getDeliveryMode());
            assertEquals(evento.getTipoEvento(), msg.getMessageProperties().getHeader("__TypeId__"));
            assertEquals(evento.getPayload(), new String(msg.getBody(), java.nio.charset.StandardCharsets.UTF_8));
            ((CorrelationData) c.getArgument(3)).getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        assertTrue(publisher.publicarSiguiente());
        assertNotNull(evento.getPublicadoEn());
        assertNull(evento.getUltimoError());
        assertEquals(1, evento.getIntentos());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TIMEOUT_CONFIRMACION", "NACK", "SIN_RUTA", "PUBLICACION_FALLIDA"})
    void falloConservaPendienteYProgramaReintentoSinGuardarDetallesSensibles(String tipo) {
        doAnswer(c -> {
            CorrelationData data = c.getArgument(3);
            if (tipo.equals("PUBLICACION_FALLIDA")) throw new AmqpConnectException(new IOException("dato sensible"));
            if (tipo.equals("SIN_RUTA")) data.setReturned(new ReturnedMessage(c.getArgument(2), 312,
                    "NO_ROUTE", evento.getExchange(), evento.getRoutingKey()));
            if (!tipo.equals("TIMEOUT_CONFIRMACION")) data.getFuture().complete(new CorrelationData.Confirm(!tipo.equals("NACK"), null));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        Instant antes = Instant.now();
        publisher.publicarSiguiente();
        assertNull(evento.getPublicadoEn());
        assertEquals(tipo, evento.getUltimoError());
        assertTrue(evento.getProximoIntento().isAfter(antes));
        assertEquals(1, evento.getIntentos());
        verify(repository).save(evento);
    }

    @Test
    void noSeMarcaPublicadoAntesDeRecibirElConfirm() throws Exception {
        publisher = new OutboxPublisher(repository, rabbit, 2000, 10, 80);
        var enviado = new CountDownLatch(1);
        var data = new java.util.concurrent.atomic.AtomicReference<CorrelationData>();
        doAnswer(c -> { data.set(c.getArgument(3)); enviado.countDown(); return null; })
                .when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        var executor = Executors.newSingleThreadExecutor();
        try {
            var envio = executor.submit(publisher::publicarSiguiente);
            assertTrue(enviado.await(1, TimeUnit.SECONDS));
            assertNull(evento.getPublicadoEn());
            verify(repository, never()).save(any());
            data.get().getFuture().complete(new CorrelationData.Confirm(true, null));
            assertTrue(envio.get(1, TimeUnit.SECONDS));
            assertNotNull(evento.getPublicadoEn());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void timeoutSeguidoDeRecuperacionConservaIdentidadDelEvento() {
        publisher.publicarSiguiente(); // Future sin confirmar: timeout.
        assertNull(evento.getPublicadoEn());
        String id = evento.getId().toString();
        doAnswer(c -> { ((CorrelationData) c.getArgument(3)).getFuture().complete(new CorrelationData.Confirm(true, null)); return null; })
                .when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        publisher.publicarSiguiente();
        assertEquals(id, evento.getId().toString());
        assertEquals(2, evento.getIntentos());
        assertNotNull(evento.getPublicadoEn());
    }

    @Test
    void backoffEsExponencialConLimite() {
        evento.setIntentos(100);
        Instant antes = Instant.now();
        publisher.publicarSiguiente();
        assertFalse(evento.getProximoIntento().isBefore(antes.plusMillis(80)));
        assertFalse(evento.getProximoIntento().isAfter(Instant.now().plusMillis(80)));
    }

    @Test
    void sinPendientesNoContactaBroker() {
        when(repository.siguientePendiente()).thenReturn(Optional.empty());
        assertFalse(publisher.publicarSiguiente());
        verifyNoInteractions(rabbit);
    }

    @ParameterizedTest
    @CsvSource({"1,60", "2,120", "3,240", "4,300", "5,300", "100,300"})
    void backoffDeFallosEs60_120_240_300Segundos(int fallo, long segundos) {
        publisher = new OutboxPublisher(repository, rabbit, 10, 60000, 300000);
        evento.setIntentos(fallo - 1);
        doAnswer(c -> { ((CorrelationData) c.getArgument(3)).getFuture().complete(new CorrelationData.Confirm(false, null)); return null; })
                .when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        Instant antes = Instant.now();
        publisher.publicarSiguiente();
        assertNull(evento.getPublicadoEn());
        assertEquals(fallo, evento.getIntentos());
        assertFalse(evento.getProximoIntento().isBefore(antes.plusSeconds(segundos)));
        assertFalse(evento.getProximoIntento().isAfter(Instant.now().plusSeconds(segundos)));
    }
}
