package com.microservice.notificaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TurnoEvent {
    private String emailDestino;
    private String nombreCliente;
    private String nombreNegocio;
    private String direccionNegocio;
    private String nombreRecurso;
    private String nombreServicio;
    private String fechaHoraTurno;
    private String tipoEvento;
}