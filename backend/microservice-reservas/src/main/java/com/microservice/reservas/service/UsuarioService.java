package com.microservice.reservas.service;

import com.microservice.reservas.dto.event.UsuarioEliminadoEvent;
import com.microservice.reservas.dto.request.ActualizarPerfilRequest;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.enums.EstadoAsociacion;
import com.microservice.reservas.enums.EstadoCuenta;
import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TurnoRepository turnoRepository;
    private final NegocioRepository negocioRepository;
    private final NegocioService negocioService;
    private final RecursoRepository recursoRepository;
    private final OutboxService outboxService;

    public Usuario actualizarPerfil(String emailLogueado, ActualizarPerfilRequest request) {
        Usuario usuario = usuarioRepository.findByEmailAndEstadoCuenta(emailLogueado, EstadoCuenta.ACTIVO)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setTelefono(request.getTelefono());

        return usuarioRepository.save(usuario);
    }

    public List<Usuario> obtenerTodos() {
        throw new AccessDeniedException("Acceso denegado: No está permitido listar datos personales de usuarios.");
    }

    public Usuario obtenerMiPerfil(String emailLogueado) {
        return usuarioRepository.findByEmailAndEstadoCuenta(emailLogueado, EstadoCuenta.ACTIVO)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado o inactivo."));
    }

    public Usuario obtenerPorId(Long id, String emailLogueado) {
        Usuario propio = obtenerMiPerfil(emailLogueado);
        if (!propio.getId().equals(id)) {
            throw new AccessDeniedException("Acceso denegado: Solo podés consultar tu propio perfil.");
        }
        return propio;
    }

    public Usuario obtenerPorEmail(String email, String emailLogueado) {
        if (!emailLogueado.equals(email)) {
            throw new AccessDeniedException("Acceso denegado: Solo podés consultar tu propio perfil.");
        }
        return obtenerMiPerfil(emailLogueado);
    }

    @Transactional
    public String eliminarUsuario(Long id, String emailLogueado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El usuario no existe."));

        if (!usuario.getEmail().equals(emailLogueado)) {
            throw new RuntimeException("Acceso denegado: Solo podés eliminar tu propia cuenta.");
        }

        if (usuario.getEstadoCuenta() != EstadoCuenta.ACTIVO) {
            throw new RuntimeException("Ya estaba eliminado.");
        }

        usuario.setEstadoCuenta(EstadoCuenta.SUSPENDIDO);
        usuarioRepository.save(usuario);

        List<EstadoTurno> estadosACancelar = List.of(EstadoTurno.CONFIRMADO);
        List<Turno> turnosAfectados = turnoRepository.findByClienteIdAndEstadoIn(id, estadosACancelar);

        for (Turno turno : turnosAfectados) {
            turno.setEstado(EstadoTurno.CANCELADO);
            turnoRepository.save(turno);
        }

        List<Negocio> negociosDelUsuario = negocioRepository.findByOwnerIdAndActivoTrue(id);
        int negociosBorrados = 0;

        for (Negocio negocio : negociosDelUsuario) {
            negocioService.eliminarNegocio(negocio.getId(), emailLogueado);
            negociosBorrados++;
        }

        List<Recurso> recursosAsignados = recursoRepository.findByUsuarioEmailAndActivoTrueOrderByNombreAsc(usuario.getEmail());
        int recursosDesvinculados = 0;

        for (Recurso recurso : recursosAsignados) {
            recurso.setUsuario(null);
            recurso.setTokenInvitacion(null);
            recurso.setEstadoAsociacion(EstadoAsociacion.ACTIVO);
            recursoRepository.save(recurso);
            recursosDesvinculados++;
        }

        outboxService.encolar("notificaciones.exchange", "usuario.eliminado.routing.key", new UsuarioEliminadoEvent(usuario.getEmail()));

        return "Usuario eliminado lógicamente. Se cancelaron " + turnosAfectados.size() +
                " turnos como cliente, se dieron de baja " + negociosBorrados +
                " negocios y se desvinculó de " + recursosDesvinculados + " agendas de trabajo.";
    }
}
