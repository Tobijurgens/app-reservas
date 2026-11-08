package com.microservice.reservas.util;

import com.microservice.reservas.dto.request.HorarioSemanalDTO;
import java.util.List;
import java.time.LocalDate;

public class ValidacionUtil {
    public static void validarSemanaCompleta(List<HorarioSemanalDTO> horarios) {
        if (horarios == null || horarios.size() != 7) {
            throw new RuntimeException("Debe enviar la configuración completa para los 7 días de la semana.");
        }

        if (horarios.stream().anyMatch(h -> h == null || h.getDiaSemana() == null || h.getActivo() == null)) {
            throw new RuntimeException("Cada horario debe indicar el día y si está activo.");
        }

        long diasUnicos = horarios.stream().map(HorarioSemanalDTO::getDiaSemana).distinct().count();
        if (diasUnicos != 7) {
            throw new RuntimeException("Hay días duplicados o faltantes en la configuración semanal.");
        }
        for (HorarioSemanalDTO horario : horarios) {
            if (Boolean.TRUE.equals(horario.getActivo())) {
                JornadaUtil.bloques(LocalDate.of(2024, 1, 1), horario.getHoraInicio(), horario.getHoraFin(),
                        horario.getHoraInicioDescanso(), horario.getHoraFinDescanso());
            }
        }
    }
}
