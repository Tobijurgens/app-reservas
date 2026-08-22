package com.microservice.reservas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HorarioDisponibleDTO {
    private String hora;         // Para mostrar en pantalla (Ej: "02:00")
    private String fechaHoraISO; // La fecha real para la BD (Ej: "2026-08-15T02:00:00")
    private boolean disponible;
}