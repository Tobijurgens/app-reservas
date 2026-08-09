package com.microservice.reservas.dto.response;

import com.microservice.reservas.entity.Recurso;
import lombok.Data;

@Data
public class RecursoResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private Long negocioId;
    private String emailUsuario;

    public RecursoResponseDTO(Recurso recurso) {
        this.id = recurso.getId();
        this.nombre = recurso.getNombre();
        this.descripcion = recurso.getDescripcion();
        if (recurso.getNegocio() != null) {
            this.negocioId = recurso.getNegocio().getId();
        }
        if (recurso.getUsuario() != null) {
            this.emailUsuario = recurso.getUsuario().getEmail();
        }
    }
}