package com.microservice.notificaciones.listener;

import com.microservice.notificaciones.config.RabbitMQConfig;
import com.microservice.notificaciones.dto.UsuarioRegistradoEvent;
import com.microservice.notificaciones.entity.HistorialNotificacion;
import com.microservice.notificaciones.enums.EstadoNotificacion;
import com.microservice.notificaciones.enums.TipoNotificacion;
import com.microservice.notificaciones.repository.HistorialNotificacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionListener {

    private final HistorialNotificacionRepository historialRepository;

    // Esta anotación hace que el método escuche permanentemente la cola
    @RabbitListener(queues = RabbitMQConfig.COLA_REGISTRO)
    public void procesarRegistroUsuario(UsuarioRegistradoEvent evento) {
        log.info("📩 [RABBITMQ] Nuevo evento recibido para: {}", evento.getEmail());
        log.info("🔑 Token mágico generado: {}", evento.getToken());

        // TODO: En el próximo paso, acá enviaremos el mail de verdad.

        // Guardamos en la base de datos de auditoría
        HistorialNotificacion historial = new HistorialNotificacion();
        historial.setEmailDestinatario(evento.getEmail());
        historial.setTipo(TipoNotificacion.VERIFICACION_CUENTA);
        historial.setEstado(EstadoNotificacion.ENVIADO); // Simulado por ahora
        historial.setFechaEnvio(LocalDateTime.now());

        historialRepository.save(historial);

        log.info("✅ Auditoría guardada en base de datos para {}", evento.getEmail());
    }
}