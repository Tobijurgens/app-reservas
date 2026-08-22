package com.microservice.reservas.dto.request;

import lombok.Data;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class HorarioSemanalDTO {
    private DayOfWeek diaSemana;
    private Boolean activo; // true si atiende, false si es franco
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private LocalTime horaInicioDescanso; // Puede ser null
    private LocalTime horaFinDescanso;    // Puede ser null
}