package com.microservice.reservas.dto;

import lombok.Data;

@Data
public class NegocioDTO {
    private String nombre;
    private String direccion;
    private String telefono;
    private Long idOwner; // El ID del usuario que está creando el negocio
}