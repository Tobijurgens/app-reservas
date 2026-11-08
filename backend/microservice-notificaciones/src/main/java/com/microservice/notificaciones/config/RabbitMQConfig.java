package com.microservice.notificaciones.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Slf4j
public class RabbitMQConfig {

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory,
            @Value("${app.notificaciones.ingest-retry-ms:1000}") long retryMs) {
        org.springframework.util.Assert.isTrue(retryMs > 0 && retryMs <= 10000, "Pausa de recepción inválida.");
        var factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(new org.springframework.amqp.support.converter.SimpleMessageConverter() {
            @Override
            public Object fromMessage(org.springframework.amqp.core.Message message) { return message.getBody(); }
        });
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setPrefetchCount(1);
        factory.setDefaultRequeueRejected(true);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless().maxRetries(3)
                .backOffOptions(retryMs, 2, retryMs * 4)
                .recoverer((message, cause) -> {
                    // La DB todavía no tomó responsabilidad. No descartar ni imprimir el mensaje original.
                    var tipo = switch (String.valueOf(message.getMessageProperties().getConsumerQueue())) {
                        case COLA_INVITACION -> com.microservice.notificaciones.enums.TipoEventoRecibido.INVITACION;
                        case COLA_TURNO -> com.microservice.notificaciones.enums.TipoEventoRecibido.TURNO;
                        case COLA_AVISOS -> com.microservice.notificaciones.enums.TipoEventoRecibido.AVISO;
                        case COLA_ELIMINACION -> com.microservice.notificaciones.enums.TipoEventoRecibido.ELIMINACION;
                        default -> com.microservice.notificaciones.enums.TipoEventoRecibido.REGISTRO;
                    };
                    log.warn("Recepcion sin persistir id={} error=PERSISTENCIA_NO_DISPONIBLE; se conserva en RabbitMQ",
                            com.microservice.notificaciones.service.IdentidadEvento.obtener(message, tipo));
                    var reentrega = new ImmediateRequeueAmqpException("Persistencia no disponible; reentrega controlada.");
                    reentrega.setStackTrace(new StackTraceElement[0]);
                    throw reentrega;
                }).build());
        // El contenedor vuelve a lanzar la excepción original y hace NACK/requeue. Se evita su logger
        // predeterminado, que puede incluir cuerpo, tokens y datos del mensaje en el stack.
        factory.setErrorHandler(error -> {});
        return factory;
    }

    public static final String COLA_REGISTRO = "registro.email.queue";
    public static final String EXCHANGE_NOTIFICACIONES = "notificaciones.exchange";
    public static final String ROUTING_KEY_REGISTRO = "registro.email.routing.key";
    public static final String COLA_INVITACION = "invitacion.email.queue";
    public static final String ROUTING_KEY_INVITACION = "invitacion.email.routing.key";
    public static final String COLA_TURNO = "turno.cliente.queue";
    public static final String ROUTING_KEY_TURNO = "turno.cliente.routing.key";
    public static final String COLA_AVISOS = "avisos.internos.queue";
    public static final String ROUTING_KEY_AVISOS = "avisos.internos.routing.key";
    public static final String COLA_ELIMINACION = "usuario.eliminado.queue";
    public static final String ROUTING_KEY_ELIMINACION = "usuario.eliminado.routing.key";

    @Bean
    public Queue registroQueue() {
        return new Queue(COLA_REGISTRO, true);
    }

    @Bean
    public Queue invitacionQueue() {
        return new Queue(COLA_INVITACION, true);
    }

    @Bean
    public Queue turnoQueue() {
        return new Queue(COLA_TURNO, true);
    }

    @Bean
    public DirectExchange notificacionesExchange() {
        return new DirectExchange(EXCHANGE_NOTIFICACIONES);
    }

    @Bean
    public Binding bindingRegistro(Queue registroQueue, DirectExchange notificacionesExchange) {
        return BindingBuilder.bind(registroQueue).to(notificacionesExchange).with(ROUTING_KEY_REGISTRO);
    }

    @Bean
    public Binding bindingInvitacion(Queue invitacionQueue, DirectExchange notificacionesExchange) {
        return BindingBuilder.bind(invitacionQueue).to(notificacionesExchange).with(ROUTING_KEY_INVITACION);
    }

    @Bean
    public Binding bindingTurno(Queue turnoQueue, DirectExchange notificacionesExchange) {
        return BindingBuilder.bind(turnoQueue).to(notificacionesExchange).with(ROUTING_KEY_TURNO);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public Queue avisosQueue() {
        return new Queue(COLA_AVISOS, true);
    }

    @Bean
    public Binding bindingAvisos(Queue avisosQueue, DirectExchange notificacionesExchange) {
        return BindingBuilder.bind(avisosQueue).to(notificacionesExchange).with(ROUTING_KEY_AVISOS);
    }

    @Bean
    public Queue eliminacionQueue() {
        return new Queue(COLA_ELIMINACION, true);
    }

    @Bean
    public Binding bindingEliminacion(Queue eliminacionQueue, DirectExchange notificacionesExchange) {
        return BindingBuilder.bind(eliminacionQueue).to(notificacionesExchange).with(ROUTING_KEY_ELIMINACION);
    }
}
