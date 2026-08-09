package com.microservice.reservas.dto.response;

import com.microservice.reservas.entity.Usuario;
import lombok.Data;

@Data
public class UsuarioResponseDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    // ¡Ojo! NO incluimos el password acá. Así lo protegemos.

    public UsuarioResponseDTO(Usuario usuario) {
        this.id = usuario.getId();
        this.nombre = usuario.getNombre();
        this.apellido = usuario.getApellido();
        this.email = usuario.getEmail();
        this.telefono = usuario.getTelefono();
    }
}