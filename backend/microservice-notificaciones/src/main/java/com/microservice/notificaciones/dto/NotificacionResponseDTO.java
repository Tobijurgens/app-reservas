package com.microservice.notificaciones.dto;

import com.microservice.notificaciones.entity.HistorialNotificacion;
import com.microservice.notificaciones.enums.EstadoNotificacion;
import com.microservice.notificaciones.enums.TipoNotificacion;

import java.time.LocalDateTime;

public record NotificacionResponseDTO(
        Long id,
        TipoNotificacion tipo,
        EstadoNotificacion estado,
        LocalDateTime fechaEnvio,
        String titulo,
        String mensaje,
        boolean leida,
        boolean soloInApp
) {
    public NotificacionResponseDTO(HistorialNotificacion notificacion) {
        this(notificacion.getId(), notificacion.getTipo(), notificacion.getEstado(),
                notificacion.getFechaEnvio(), notificacion.getTitulo(), notificacion.getMensaje(),
                notificacion.isLeida(), notificacion.isSoloInApp());
    }
}
