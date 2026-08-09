package com.microservice.reservas.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TurnoDTO {

    @NotNull(message = "El servicio es obligatorio")
    private Long idServicio;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Future(message = "La fecha del turno debe ser a futuro")
    private LocalDateTime fechaHoraInicio;
}