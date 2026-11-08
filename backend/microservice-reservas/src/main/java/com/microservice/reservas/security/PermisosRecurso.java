package com.microservice.reservas.security;

import com.microservice.reservas.entity.Recurso;
import com.microservice.reservas.enums.EstadoAsociacion;
import org.springframework.security.access.AccessDeniedException;

public final class PermisosRecurso {

    private PermisosRecurso() {
    }

    public static boolean esPropietario(Recurso recurso, String email) {
        return recurso.getNegocio().getOwner().getEmail().equals(email);
    }

    public static boolean esEmpleadoActivo(Recurso recurso, String email) {
        return Boolean.TRUE.equals(recurso.getActivo())
                && recurso.getEstadoAsociacion() == EstadoAsociacion.ACTIVO
                && recurso.getUsuario() != null
                && recurso.getUsuario().isEnabled()
                && recurso.getUsuario().getEmail().equals(email);
    }

    public static void validarGestion(Recurso recurso, String email) {
        if (!esPropietario(recurso, email) && !esEmpleadoActivo(recurso, email)) {
            throw new AccessDeniedException("Acceso denegado: Se requiere ser propietario o empleado con asociación activa de este recurso.");
        }
    }
}
