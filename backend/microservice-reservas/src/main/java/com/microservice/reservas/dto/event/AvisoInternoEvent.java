package com.microservice.reservas.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvisoInternoEvent {
    private String emailDestino;
    private String titulo;
    private String mensaje;
}