package com.microservice.reservas.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TurnoDTO {
    private Long idCliente;
    private Long idRecurso;
    private Long idServicio;
    private LocalDateTime fechaHoraInicio;
}