package com.microservice.reservas.util;

import com.microservice.reservas.exception.TurnoException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** El día semanal identifica la fecha de inicio de la jornada, también si termina al día siguiente. */
public final class JornadaUtil {
    private JornadaUtil() {}

    public record Intervalo(LocalDateTime inicio, LocalDateTime fin) {
        public boolean contiene(LocalDateTime desde, LocalDateTime hasta) {
            return desde.isBefore(hasta) && !desde.isBefore(inicio) && !hasta.isAfter(fin);
        }

        public Intervalo interseccion(Intervalo otro) {
            LocalDateTime desde = inicio.isAfter(otro.inicio) ? inicio : otro.inicio;
            LocalDateTime hasta = fin.isBefore(otro.fin) ? fin : otro.fin;
            return desde.isBefore(hasta) ? new Intervalo(desde, hasta) : null;
        }
    }

    public static Intervalo jornada(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        if (inicio == null || fin == null || inicio.equals(fin)) {
            throw new TurnoException(HttpStatus.BAD_REQUEST, "La jornada debe tener inicio y fin diferentes.");
        }
        LocalDateTime hasta = fecha.atTime(fin);
        if (fin.isBefore(inicio)) hasta = hasta.plusDays(1);
        return new Intervalo(fecha.atTime(inicio), hasta);
    }

    public static List<Intervalo> bloques(LocalDate fecha, LocalTime inicio, LocalTime fin,
            LocalTime inicioDescanso, LocalTime finDescanso) {
        Intervalo jornada = jornada(fecha, inicio, fin);
        if (inicioDescanso == null && finDescanso == null) return List.of(jornada);
        if (inicioDescanso == null || finDescanso == null) {
            throw new TurnoException(HttpStatus.BAD_REQUEST, "El descanso debe tener inicio y fin.");
        }
        LocalDateTime desde = fecha.atTime(inicioDescanso);
        LocalDateTime hasta = fecha.atTime(finDescanso);
        if (inicioDescanso.isBefore(inicio)) desde = desde.plusDays(1);
        if (finDescanso.isBefore(inicio)) hasta = hasta.plusDays(1);
        if (!jornada.contiene(desde, hasta)) {
            throw new TurnoException(HttpStatus.BAD_REQUEST, "El descanso debe estar contenido en la jornada.");
        }
        return List.of(new Intervalo(jornada.inicio(), desde), new Intervalo(hasta, jornada.fin())).stream()
                .filter(b -> b.inicio().isBefore(b.fin())).toList();
    }
}
