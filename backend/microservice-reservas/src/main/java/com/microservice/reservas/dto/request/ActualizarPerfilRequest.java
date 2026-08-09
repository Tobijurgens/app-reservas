package com.microservice.reservas.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActualizarPerfilRequest {

    @NotBlank(message = "El nombre no puede estar vacío")
    private String nombre;

    @NotBlank(message = "El apellido no puede estar vacío")
    private String apellido;

    @NotBlank(message = "El teléfono no puede estar vacío")
    private String telefono;
}