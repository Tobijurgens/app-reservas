package com.microservice.reservas.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class RecursoDTO {

    @NotBlank(message = "El nombre del recurso es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "El ID del negocio es obligatorio")
    @Positive(message = "El ID del negocio debe ser mayor a 0")
    private Long idNegocio;

    @Email(message = "El formato del email no es válido")
    private String emailUsuario;

    @NotEmpty(message = "Debe configurar los horarios de la semana")
    private List<HorarioSemanalDTO> horarios;
}