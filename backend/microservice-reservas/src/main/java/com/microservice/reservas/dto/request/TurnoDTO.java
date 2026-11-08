package com.microservice.reservas.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TurnoDTO {

    @NotNull(message = "El servicio es obligatorio")
    @Positive(message = "El identificador del servicio debe ser positivo")
    private Long idServicio;

    // Opcional para conservar clientes anteriores: si falta, se deriva del servicio.
    @Positive(message = "El identificador del recurso debe ser positivo")
    private Long idRecurso;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Future(message = "La fecha del turno debe ser a futuro")
    private LocalDateTime fechaHoraInicio;
}
