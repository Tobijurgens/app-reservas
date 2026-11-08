package com.microservice.reservas.dto.response;

import java.time.LocalDateTime;

public record InvitacionResponseDTO(String nombreNegocio, String nombreRecurso, LocalDateTime fechaExpiracion) {
}
