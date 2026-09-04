package com.microservice.reservas.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitacionEmpleadoEvent {
    private String emailDestino;
    private String nombreNegocio;
    private String nombreRecurso;
    private String token;
}