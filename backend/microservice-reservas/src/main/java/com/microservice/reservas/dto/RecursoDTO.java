package com.microservice.reservas.dto;

import lombok.Data;

@Data
public class RecursoDTO {
    private String nombre; // <-- Nuevo
    private String descripcion;
    private Long idNegocio;
    private String emailUsuario; // Ahora es opcional
}