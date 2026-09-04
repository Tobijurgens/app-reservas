package com.microservice.reservas.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
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