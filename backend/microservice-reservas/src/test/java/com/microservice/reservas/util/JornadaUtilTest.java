package com.microservice.reservas.util;

import com.microservice.reservas.dto.request.HorarioSemanalDTO;
import com.microservice.reservas.exception.TurnoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.*;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JornadaUtilTest {
    private final LocalDate fecha = LocalDate.of(2026, 11, 6);

    @Test
    void descansoNocturnoSeConvierteEnDosBloquesConFechasCompletas() {
        var bloques = JornadaUtil.bloques(fecha, LocalTime.of(22, 0), LocalTime.of(2, 0),
                LocalTime.of(23, 45), LocalTime.of(0, 15));
        assertEquals(List.of(
                new JornadaUtil.Intervalo(fecha.atTime(22, 0), fecha.atTime(23, 45)),
                new JornadaUtil.Intervalo(fecha.plusDays(1).atTime(0, 15), fecha.plusDays(1).atTime(2, 0))), bloques);
    }

    @ParameterizedTest
    @CsvSource({"09:00,18:00,12:00,13:00", "22:00,02:00,23:45,00:15", "22:00,02:00,00:30,01:00"})
    void limitesDeDescansoSonSemiabiertos(String inicio, String fin, String descansoInicio, String descansoFin) {
        var bloques = JornadaUtil.bloques(fecha, LocalTime.parse(inicio), LocalTime.parse(fin),
                LocalTime.parse(descansoInicio), LocalTime.parse(descansoFin));
        var antes = bloques.get(0);
        var despues = bloques.get(1);
        assertTrue(antes.contiene(antes.fin().minusMinutes(15), antes.fin()));
        assertTrue(despues.contiene(despues.inicio(), despues.inicio().plusMinutes(15)));
        assertNull(antes.interseccion(despues));
    }

    @ParameterizedTest
    @CsvSource({"09:00,18:00,12:00,", "09:00,18:00,18:00,19:00", "09:00,18:00,13:00,12:00",
            "22:00,02:00,01:30,02:30", "22:00,02:00,23:30,23:30", "09:00,09:00,,"})
    void configuracionesInvalidasNoSeGuardan(String inicio, String fin, String desde, String hasta) {
        var semana = Arrays.stream(DayOfWeek.values()).map(d -> {
            var h = new HorarioSemanalDTO();
            h.setDiaSemana(d);
            h.setActivo(true);
            h.setHoraInicio(LocalTime.parse(inicio));
            h.setHoraFin(LocalTime.parse(fin));
            h.setHoraInicioDescanso(desde == null ? null : LocalTime.parse(desde));
            h.setHoraFinDescanso(hasta == null ? null : LocalTime.parse(hasta));
            return h;
        }).toList();
        assertThrows(TurnoException.class, () -> ValidacionUtil.validarSemanaCompleta(semana));
    }
}
