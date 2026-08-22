package com.microservice.reservas.service;

import com.microservice.reservas.dto.request.ActualizarPerfilRequest;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.enums.EstadoCuenta;
import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TurnoRepository turnoRepository;

    // Inyectamos esto para aplicar la cascada de borrado
    private final NegocioRepository negocioRepository;
    private final NegocioService negocioService;

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

    public String eliminarUsuario(Long id, String emailLogueado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El usuario no existe."));

        // 1. CANDADO: Solo podés borrar tu propia cuenta
        if (!usuario.getEmail().equals(emailLogueado)) {
            throw new RuntimeException("Acceso denegado: Solo podés eliminar tu propia cuenta.");
        }

        if (usuario.getEstadoCuenta() != EstadoCuenta.ACTIVO) {
            throw new RuntimeException("Ya estaba eliminado.");
        }

        // 2. Baja lógica del usuario
        usuario.setEstadoCuenta(EstadoCuenta.SUSPENDIDO);
        usuarioRepository.save(usuario);

        // 3. Cancelar los turnos que haya sacado como cliente
        List<EstadoTurno> estadosACancelar = List.of(EstadoTurno.CONFIRMADO);
        List<Turno> turnosAfectados = turnoRepository.findByClienteIdAndEstadoIn(id, estadosACancelar);

        for (Turno turno : turnosAfectados) {
            turno.setEstado(EstadoTurno.CANCELADO);
            turnoRepository.save(turno);
        }

        // 4. Cascada: Borrar todos los negocios de los que es dueño
        List<Negocio> negociosDelUsuario = negocioRepository.findByOwnerIdAndActivoTrue(id);
        int negociosBorrados = 0;

        for (Negocio negocio : negociosDelUsuario) {
            // Reutilizamos toda la magia de borrado que ya armamos antes
            negocioService.eliminarNegocio(negocio.getId(), emailLogueado);
            negociosBorrados++;
        }

        return "Usuario eliminado lógicamente. Se cancelaron " + turnosAfectados.size() +
                " turnos como cliente y se dieron de baja " + negociosBorrados + " negocios asociados.";
    }
}