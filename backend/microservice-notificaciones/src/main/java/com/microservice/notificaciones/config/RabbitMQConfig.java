package com.microservice.notificaciones.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String COLA_REGISTRO = "registro.email.queue";
    public static final String EXCHANGE_NOTIFICACIONES = "notificaciones.exchange";
    public static final String ROUTING_KEY_REGISTRO = "registro.email.routing.key";
    public static final String COLA_INVITACION = "invitacion.email.queue";
    public static final String ROUTING_KEY_INVITACION = "invitacion.email.routing.key";
    public static final String COLA_TURNO = "turno.cliente.queue";
    public static final String ROUTING_KEY_TURNO = "turno.cliente.routing.key";

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
}