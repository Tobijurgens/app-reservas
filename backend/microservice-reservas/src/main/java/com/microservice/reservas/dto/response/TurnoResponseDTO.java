package com.microservice.reservas.dto.response;

import com.microservice.reservas.entity.Turno;
import com.microservice.reservas.enums.EstadoTurno;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TurnoResponseDTO {
    private Long id;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private EstadoTurno estado;
    private Long servicioId;
    private String nombreServicio;
    private String emailCliente;

    public TurnoResponseDTO(Turno turno) {
        this.id = turno.getId();
        this.fechaHoraInicio = turno.getFechaHoraInicio();
        this.fechaHoraFin = turno.getFechaHoraFin();
        this.estado = turno.getEstado();
        if (turno.getServicio() != null) {
            this.servicioId = turno.getServicio().getId();
            this.nombreServicio = turno.getServicio().getNombre();
        }
        if (turno.getCliente() != null) {
            this.emailCliente = turno.getCliente().getEmail();
        }
    }
}