package com.microservice.reservas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Email;
import lombok.Data;

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
}