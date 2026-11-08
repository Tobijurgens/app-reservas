package com.microservice.notificaciones.service;

import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.AddressException;
import org.springframework.mail.*;
import org.thymeleaf.exceptions.TemplateProcessingException;
import tools.jackson.core.JacksonException;

import java.net.SocketTimeoutException;
import java.util.*;

/** Clasifica sin guardar mensajes de excepción que puedan contener destinatarios, tokens o credenciales. */
public record FalloEnvio(boolean permanente, String codigo) {
    public static FalloEnvio clasificar(Throwable error) {
        Deque<Throwable> pendientes = new ArrayDeque<>();
        Set<Throwable> vistos = Collections.newSetFromMap(new IdentityHashMap<>());
        pendientes.add(error);
        String temporal = "SMTP_TEMPORAL";
        boolean rechazoTemporal = false;
        boolean destinatarioInvalido = false;
        while (!pendientes.isEmpty()) {
            Throwable actual = pendientes.removeFirst();
            if (!vistos.add(actual)) continue;
            if (actual instanceof JacksonException) return new FalloEnvio(true, "JSON_INVALIDO");
            if (actual instanceof TemplateProcessingException) return new FalloEnvio(true, "PLANTILLA_INVALIDA");
            if (actual instanceof AddressException) return new FalloEnvio(true, "DESTINATARIO_INVALIDO");
            if (actual instanceof MailAuthenticationException) return new FalloEnvio(true, "SMTP_AUTENTICACION");
            if (actual instanceof MailPreparationException || actual instanceof MailParseException
                    || actual instanceof IllegalArgumentException) return new FalloEnvio(true, "MENSAJE_INVALIDO");
            Integer retorno = codigoSmtp(actual);
            if (retorno != null && retorno >= 500) return new FalloEnvio(true, "SMTP_RECHAZO_PERMANENTE");
            if (retorno != null && retorno >= 400) { temporal = "SMTP_RECHAZO_TEMPORAL"; rechazoTemporal = true; }
            else if (actual instanceof SendFailedException envio && envio.getInvalidAddresses() != null
                    && envio.getInvalidAddresses().length > 0) destinatarioInvalido = true;
            if (actual instanceof SocketTimeoutException) temporal = "SMTP_TIMEOUT";
            if (actual instanceof java.net.ConnectException || actual instanceof java.net.UnknownHostException) temporal = "SMTP_CONEXION";
            if (actual.getCause() != null) pendientes.add(actual.getCause());
            if (actual instanceof MessagingException mensaje && mensaje.getNextException() != null)
                pendientes.add(mensaje.getNextException());
            if (actual instanceof MailSendException envio) Collections.addAll(pendientes, envio.getMessageExceptions());
        }
        if (destinatarioInvalido && !rechazoTemporal) return new FalloEnvio(true, "DESTINATARIO_INVALIDO");
        return new FalloEnvio(false, temporal);
    }

    private static Integer codigoSmtp(Throwable error) {
        // JavaMail/Angus expone este método en sus excepciones SMTP. No agrega dependencia al proveedor.
        try {
            Object codigo = error.getClass().getMethod("getReturnCode").invoke(error);
            return codigo instanceof Number numero ? numero.intValue() : null;
        } catch (ReflectiveOperationException | SecurityException ignored) { return null; }
    }
}
