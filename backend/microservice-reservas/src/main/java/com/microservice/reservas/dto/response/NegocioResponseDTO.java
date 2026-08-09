package com.microservice.reservas.dto.response;

import com.microservice.reservas.entity.Negocio;
import lombok.Data;

@Data
public class NegocioResponseDTO {
    private Long id;
    private String nombre;
    private String direccion;
    private String telefono;

    public NegocioResponseDTO(Negocio negocio) {
        this.id = negocio.getId();
        this.nombre = negocio.getNombre();
        this.direccion = negocio.getDireccion();
        this.telefono = negocio.getTelefono();
    }
}