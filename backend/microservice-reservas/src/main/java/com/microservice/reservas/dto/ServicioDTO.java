package com.microservice.reservas.dto;

import lombok.Data;

@Data
public class ServicioDTO {
    private String nombre;
    private String descripcion;
    private Double precio;
    private Integer duracionMinutos;
    private Long idRecurso;
}