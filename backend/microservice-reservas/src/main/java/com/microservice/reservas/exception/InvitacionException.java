package com.microservice.reservas.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvitacionException extends RuntimeException {
    private final HttpStatus status;

    public InvitacionException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
