package com.microservice.reservas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ServicioDTO {

    @NotBlank(message = "El nombre del servicio es obligatorio")
    private String nombre;

    private String descripcion;

    @Positive(message = "El precio debe ser mayor a 0")
    private Double precio;

    @NotNull(message = "La duración es obligatoria")
    @Positive(message = "La duración en minutos debe ser mayor a 0")
    private Integer duracionMinutos;

    @NotNull(message = "Debe asociar este servicio a un recurso")
    @Positive(message = "El ID del recurso no es válido")
    private Long idRecurso;
}