package com.microservice.notificaciones.listener;

import com.microservice.notificaciones.config.RabbitMQConfig;
import com.microservice.notificaciones.dto.*;
import com.microservice.notificaciones.entity.HistorialNotificacion;
import com.microservice.notificaciones.enums.EstadoNotificacion;
import com.microservice.notificaciones.enums.TipoNotificacion;
import com.microservice.notificaciones.repository.HistorialNotificacionRepository;
import com.microservice.notificaciones.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionListener {

    private final HistorialNotificacionRepository historialRepository;
    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.COLA_REGISTRO)
    public void procesarRegistroUsuario(UsuarioRegistradoEvent evento) {
        log.info("📩 [RABBITMQ] Procesando registro para: {}", evento.getEmail());

        HistorialNotificacion historial = new HistorialNotificacion();
        historial.setEmailDestinatario(evento.getEmail());
        historial.setTipo(TipoNotificacion.VERIFICACION_CUENTA);
        historial.setFechaEnvio(LocalDateTime.now());
        historial.setTitulo("¡Bienvenido a AgendApp!");
        historial.setMensaje("¡Qué bueno tenerte acá! Ya podés empezar a gestionar tus turnos y organizar tu agenda.");
        historial.setSoloInApp(false);

        try {
            emailService.enviarCorreoVerificacion(
                    evento.getEmail(),
                    evento.getNombre(),
                    evento.getToken()
            );

            historial.setEstado(EstadoNotificacion.ENVIADO);
            log.info("✅ Auditoría: Guardado como ENVIADO para {}", evento.getEmail());

        } catch (Exception e) {
            historial.setEstado(EstadoNotificacion.ERROR);
            historial.setDetalleError(e.getMessage());
            log.error("❌ Auditoría: Guardado como ERROR para {}", evento.getEmail());
        }

        historialRepository.save(historial);
    }

    @RabbitListener(queues = RabbitMQConfig.COLA_INVITACION)
    public void procesarInvitacionEmpleado(InvitacionEmpleadoEvent evento) {
        log.info("📩 [RABBITMQ] Procesando invitación para: {}", evento.getEmailDestino());

        HistorialNotificacion historial = new HistorialNotificacion();
        historial.setEmailDestinatario(evento.getEmailDestino());
        historial.setTipo(TipoNotificacion.INVITACION_EMPLEADO);
        historial.setFechaEnvio(LocalDateTime.now());
        historial.setTitulo("Nueva Invitación de Staff");
        historial.setMensaje(evento.getNombreNegocio() + " te invitó a unirte como " + evento.getNombreRecurso() + ". Revisa la bandeja de entrada de tu correo");
        historial.setSoloInApp(false);

        try {
            emailService.enviarCorreoInvitacion(
                    evento.getEmailDestino(),
                    evento.getNombreNegocio(),
                    evento.getNombreRecurso(),
                    evento.getToken()
            );

            historial.setEstado(EstadoNotificacion.ENVIADO);
            log.info("✅ Auditoría: Invitación ENVIADA para {}", evento.getEmailDestino());

        } catch (Exception e) {
            historial.setEstado(EstadoNotificacion.ERROR);
            historial.setDetalleError(e.getMessage());
            log.error("❌ Auditoría: ERROR al enviar invitación para {}", evento.getEmailDestino());
        }

        historialRepository.save(historial);
    }

    @RabbitListener(queues = RabbitMQConfig.COLA_TURNO)
    public void procesarTurno(TurnoEvent evento) {
        log.info("📩 [RABBITMQ] Procesando evento de turno para: {}", evento.getEmailDestino());

        HistorialNotificacion historial = new HistorialNotificacion();
        historial.setEmailDestinatario(evento.getEmailDestino());
        historial.setTipo(TipoNotificacion.TURNO_CLIENTE);
        historial.setFechaEnvio(LocalDateTime.now());

        boolean esConfirmacion = "CONFIRMACION".equals(evento.getTipoEvento());
        historial.setTitulo(esConfirmacion ? "Turno Confirmado" : "Turno Cancelado");

        String estadoMensaje = esConfirmacion ? "confirmado" : "cancelado";
        historial.setMensaje("Tu turno en " + evento.getNombreNegocio() + " ha sido " + estadoMensaje + ".");

        historial.setSoloInApp(false);

        try {
            emailService.enviarCorreoTurno(evento);
            historial.setEstado(EstadoNotificacion.ENVIADO);
            log.info("✅ Auditoría: Correo de turno ENVIADO para {}", evento.getEmailDestino());
        } catch (Exception e) {
            historial.setEstado(EstadoNotificacion.ERROR);
            historial.setDetalleError(e.getMessage());
            log.error("❌ Auditoría: ERROR al enviar correo de turno para {}", evento.getEmailDestino());
        }

        historialRepository.save(historial);
    }

    @RabbitListener(queues = RabbitMQConfig.COLA_AVISOS)
    public void procesarAvisoInterno(AvisoInternoEvent evento) {
        log.info("📩 [RABBITMQ] Procesando aviso in-app para: {}", evento.getEmailDestino());

        HistorialNotificacion historial = new HistorialNotificacion();
        historial.setEmailDestinatario(evento.getEmailDestino());
        historial.setTipo(TipoNotificacion.ALERTA_INTERNA);
        historial.setFechaEnvio(LocalDateTime.now());
        historial.setTitulo(evento.getTitulo());
        historial.setMensaje(evento.getMensaje());
        historial.setSoloInApp(true);
        historial.setEstado(EstadoNotificacion.ENVIADO);

        historialRepository.save(historial);
    }

    @RabbitListener(queues = RabbitMQConfig.COLA_ELIMINACION)
    @Transactional
    public void procesarEliminacionUsuario(UsuarioEliminadoEvent evento) {
        log.info("🗑️ [RABBITMQ] Borrando historial de notificaciones para cuenta eliminada: {}", evento.getEmail());

        historialRepository.deleteAllByEmailDestinatario(evento.getEmail());
    }
}