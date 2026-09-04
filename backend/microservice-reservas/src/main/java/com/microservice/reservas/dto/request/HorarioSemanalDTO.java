package com.microservice.reservas.dto.request;

import lombok.Data;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class HorarioSemanalDTO {
    private DayOfWeek diaSemana;
    private Boolean activo;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private LocalTime horaInicioDescanso;
    private LocalTime horaFinDescanso;
}