package com.microservice.reservas.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRegistradoEvent {
    private String email;
    private String nombre;
    private String token;
}