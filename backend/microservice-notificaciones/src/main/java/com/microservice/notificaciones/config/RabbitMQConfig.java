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

    // Nombres constantes para no equivocarnos al tipear
    public static final String COLA_REGISTRO = "registro.email.queue";
    public static final String EXCHANGE_NOTIFICACIONES = "notificaciones.exchange";
    public static final String ROUTING_KEY_REGISTRO = "registro.email.routing.key";

    // 1. Declaramos la Cola
    @Bean
    public Queue registroQueue() {
        return new Queue(COLA_REGISTRO, true); // true = la cola sobrevive a reinicios del servidor
    }

    // 2. Declaramos el Exchange (El "router" de mensajes)
    @Bean
    public DirectExchange notificacionesExchange() {
        return new DirectExchange(EXCHANGE_NOTIFICACIONES);
    }

    // 3. Conectamos la Cola con el Exchange usando la Routing Key
    @Bean
    public Binding bindingRegistro(Queue registroQueue, DirectExchange notificacionesExchange) {
        return BindingBuilder.bind(registroQueue).to(notificacionesExchange).with(ROUTING_KEY_REGISTRO);
    }

    // 4. Conversor a JSON (Magia pura para que Spring convierta los objetos automáticamente)
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}