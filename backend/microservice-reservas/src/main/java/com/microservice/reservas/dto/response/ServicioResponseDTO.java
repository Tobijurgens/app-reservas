package com.microservice.reservas.dto.response;

import com.microservice.reservas.entity.Servicio;
import lombok.Data;

@Data
public class ServicioResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private Double precio;
    private Integer duracionMinutos;
    private Long recursoId;

    public ServicioResponseDTO(Servicio servicio) {
        this.id = servicio.getId();
        this.nombre = servicio.getNombre();
        this.descripcion = servicio.getDescripcion();
        this.precio = servicio.getPrecio();
        this.duracionMinutos = servicio.getDuracionMinutos();
        if (servicio.getRecurso() != null) {
            this.recursoId = servicio.getRecurso().getId();
        }
    }
}