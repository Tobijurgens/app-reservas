package com.microservice.notificaciones.listener;

import com.microservice.notificaciones.config.RabbitMQConfig;
import com.microservice.notificaciones.enums.TipoEventoRecibido;
import com.microservice.notificaciones.service.RecepcionEventoService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificacionListener {
    private final RecepcionEventoService recepcion;

    // AUTO reconoce el mensaje solo cuando registrar retorna, después de su commit.
    // Se recibe Message crudo: un JSON inválido también se conserva y se clasifica en el worker.
    @RabbitListener(queues = RabbitMQConfig.COLA_REGISTRO)
    public void procesarRegistroUsuario(Message mensaje) { recepcion.registrar(mensaje, TipoEventoRecibido.REGISTRO); }

    @RabbitListener(queues = RabbitMQConfig.COLA_INVITACION)
    public void procesarInvitacionEmpleado(Message mensaje) { recepcion.registrar(mensaje, TipoEventoRecibido.INVITACION); }

    @RabbitListener(queues = RabbitMQConfig.COLA_TURNO)
    public void procesarTurno(Message mensaje) { recepcion.registrar(mensaje, TipoEventoRecibido.TURNO); }

    @RabbitListener(queues = RabbitMQConfig.COLA_AVISOS)
    public void procesarAvisoInterno(Message mensaje) { recepcion.registrar(mensaje, TipoEventoRecibido.AVISO); }

    @RabbitListener(queues = RabbitMQConfig.COLA_ELIMINACION)
    public void procesarEliminacionUsuario(Message mensaje) { recepcion.registrar(mensaje, TipoEventoRecibido.ELIMINACION); }
}
