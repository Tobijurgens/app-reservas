package com.microservice.reservas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InvitacionDecisionDTO(@NotBlank @Size(max = 100) String token) {
    @Override
    public String toString() {
        return "InvitacionDecisionDTO[token=REDACTADO]";
    }
}
