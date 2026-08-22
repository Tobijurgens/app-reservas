package com.microservice.reservas.dto.response;

import lombok.Data;
import java.util.Map;

@Data
public class ReporteResponseDTO {
    private int totalTurnos;
    private int turnosCompletados;
    private int turnosPendientes;
    private int turnosCancelados;
    private double ingresosTotales;
    private double tasaCancelacionPorcentaje; // Usamos decimales/porcentajes directos

    // Mapas para saber qué se vende más y quién trabaja más
    private Map<String, Long> serviciosPopulares;
    private Map<String, Long> recursosPopulares;
}