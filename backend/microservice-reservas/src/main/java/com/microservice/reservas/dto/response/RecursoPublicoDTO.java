package com.microservice.reservas.dto.response;

import com.microservice.reservas.entity.Recurso;

public record RecursoPublicoDTO(Long id, String nombre, String descripcion, Long negocioId) {
    public RecursoPublicoDTO(Recurso recurso) {
        this(recurso.getId(), recurso.getNombre(), recurso.getDescripcion(), recurso.getNegocio().getId());
    }
}
