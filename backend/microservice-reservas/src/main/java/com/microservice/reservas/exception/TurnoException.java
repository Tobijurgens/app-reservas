package com.microservice.reservas.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TurnoException extends RuntimeException {
    private final HttpStatus status;

    public TurnoException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
