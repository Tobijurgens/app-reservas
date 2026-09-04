package com.microservice.reservas.util;

import com.microservice.reservas.dto.request.HorarioSemanalDTO;
import java.util.List;

public class ValidacionUtil {
    public static void validarSemanaCompleta(List<HorarioSemanalDTO> horarios) {
        if (horarios == null || horarios.size() != 7) {
            throw new RuntimeException("Debe enviar la configuración completa para los 7 días de la semana.");
        }

        long diasUnicos = horarios.stream().map(HorarioSemanalDTO::getDiaSemana).distinct().count();
        if (diasUnicos != 7) {
            throw new RuntimeException("Hay días duplicados o faltantes en la configuración semanal.");
        }
    }
}