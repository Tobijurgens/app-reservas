package com.microservice.reservas.service;

import com.microservice.reservas.dto.request.ActualizarPerfilRequest;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.enums.EstadoAsociacion;
import com.microservice.reservas.enums.EstadoCuenta;
import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TurnoRepository turnoRepository;
    private final NegocioRepository negocioRepository;
    private final NegocioService negocioService;
    private final RecursoRepository recursoRepository;

    public Usuario actualizarPerfil(String emailLogueado, ActualizarPerfilRequest request) {
        Usuario usuario = usuarioRepository.findByEmailAndEstadoCuenta(emailLogueado, EstadoCuenta.ACTIVO)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setTelefono(request.getTelefono());

        return usuarioRepository.save(usuario);
    }

    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findByEstadoCuenta(EstadoCuenta.ACTIVO);
    }

    public Optional<Usuario> obtenerPorId(Long id) {
        return usuarioRepository.findByIdAndEstadoCuenta(id, EstadoCuenta.ACTIVO);
    }

    public Optional<Usuario> obtenerPorEmail(String email) {
        return usuarioRepository.findByEmailAndEstadoCuenta(email, EstadoCuenta.ACTIVO);
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

        List<Recurso> recursosAsignados = recursoRepository.findByUsuarioEmailAndActivoTrue(usuario.getEmail());
        int recursosDesvinculados = 0;

        for (Recurso recurso : recursosAsignados) {
            recurso.setUsuario(null);
            recurso.setTokenInvitacion(null);
            recurso.setEstadoAsociacion(EstadoAsociacion.ACTIVO); // Vuelve a ser un recurso genérico disponible
            recursoRepository.save(recurso);
            recursosDesvinculados++;
        }

        return "Usuario eliminado lógicamente. Se cancelaron " + turnosAfectados.size() +
                " turnos como cliente, se dieron de baja " + negociosBorrados +
                " negocios y se desvinculó de " + recursosDesvinculados + " agendas de trabajo.";
    }
}