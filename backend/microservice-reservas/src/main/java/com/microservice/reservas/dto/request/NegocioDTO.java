package com.microservice.reservas.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class NegocioDTO {
    @NotBlank(message = "El nombre del negocio es obligatorio")
    private String nombre;

    @NotBlank(message = "El alias es obligatorio")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "El alias solo puede contener letras minúsculas, números y guiones")
    private String alias;

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    @NotBlank(message = "El teléfono es obligatorio")
    private String telefono;

    private String descripcion;

    @Email(message = "El formato del email de contacto no es válido")
    private String emailContacto;

    @NotEmpty(message = "Debe configurar los horarios de la semana")
    private List<HorarioSemanalDTO> horarios;
}