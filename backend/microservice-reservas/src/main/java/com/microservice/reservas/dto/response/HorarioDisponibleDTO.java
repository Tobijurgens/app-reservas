package com.microservice.reservas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HorarioDisponibleDTO {
    private String hora;
    private String fechaHoraISO;
    private boolean disponible;
}