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