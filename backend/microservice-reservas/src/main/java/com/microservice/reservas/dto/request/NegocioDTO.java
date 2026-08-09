package com.microservice.reservas.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NegocioDTO {
    @NotBlank(message = "El nombre del negocio es obligatorio")
    private String nombre;

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    @NotBlank(message = "El teléfono es obligatorio")
    private String telefono;
}