package com.microservice.reservas.service;

import com.microservice.reservas.dto.request.RecursoDTO;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecursoService {

    private final RecursoRepository recursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NegocioRepository negocioRepository;
    private final ServicioRepository servicioRepository;
    private final TurnoRepository turnoRepository;

    public Recurso crearRecurso(RecursoDTO recursoDto, String emailLogueado) {
        Negocio negocio = negocioRepository.findByIdAndActivoTrue(recursoDto.getIdNegocio())
                .orElseThrow(() -> new RuntimeException("El negocio especificado no existe o está inactivo."));

        if (!negocio.getOwner().getEmail().equals(emailLogueado)) {
            throw new RuntimeException("Acceso denegado: Solo el dueño del negocio puede agregar recursos.");
        }

        Recurso recurso = new Recurso();
        recurso.setNombre(recursoDto.getNombre());
        recurso.setDescripcion(recursoDto.getDescripcion());
        recurso.setNegocio(negocio);
        recurso.setActivo(true);

        if (recursoDto.getEmailUsuario() != null && !recursoDto.getEmailUsuario().isEmpty()) {
            Usuario usuario = usuarioRepository.findByEmailAndActivoTrue(recursoDto.getEmailUsuario())
                    .orElseThrow(() -> new RuntimeException("No se encontró ningún usuario activo con el email " + recursoDto.getEmailUsuario()));
            recurso.setUsuario(usuario);
        }

        return recursoRepository.save(recurso);
    }

    public Recurso actualizarRecurso(Long id, RecursoDTO recursoDto, String emailLogueado) {
        Recurso recurso = recursoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RuntimeException("Recurso no encontrado o inactivo."));

        // CANDADO: Validar que el que edita es el dueño del negocio al que pertenece el recurso
        if (!recurso.getNegocio().getOwner().getEmail().equals(emailLogueado)) {
            throw new RuntimeException("Acceso denegado: No sos el dueño del negocio asociado a este recurso.");
        }

        recurso.setNombre(recursoDto.getNombre());
        recurso.setDescripcion(recursoDto.getDescripcion());

        return recursoRepository.save(recurso);
    }

    public List<Recurso> obtenerTodos() {
        return recursoRepository.findByActivoTrue();
    }

    public Optional<Recurso> obtenerPorId(Long id) {
        return recursoRepository.findByIdAndActivoTrue(id);
    }

    public List<Recurso> obtenerPorNegocio(Long negocioId) {
        return recursoRepository.findByNegocioIdAndActivoTrue(negocioId);
    }

    public String eliminarRecurso(Long id, String emailLogueado) {
        Recurso recurso = recursoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El recurso no existe."));

        if (!recurso.getActivo()) {
            throw new RuntimeException("Ya estaba eliminado.");
        }

        // CANDADO: Solo el dueño del negocio puede eliminar recursos
        if (!recurso.getNegocio().getOwner().getEmail().equals(emailLogueado)) {
            throw new RuntimeException("Acceso denegado: Solo el dueño del negocio puede eliminar recursos.");
        }

        recurso.setActivo(false);
        recursoRepository.save(recurso);

        List<Servicio> serviciosDelRecurso = servicioRepository.findByRecursoIdAndActivoTrue(id);
        for (Servicio servicio : serviciosDelRecurso) {
            servicio.setActivo(false);
            servicioRepository.save(servicio);
        }

        List<EstadoTurno> estadosACancelar = Arrays.asList(EstadoTurno.PENDIENTE, EstadoTurno.CONFIRMADO);
        List<Turno> turnosAfectados = turnoRepository.findByRecursoIdAndEstadoIn(id, estadosACancelar);

        for (Turno turno : turnosAfectados) {
            turno.setEstado(EstadoTurno.CANCELADO);
            turnoRepository.save(turno);
        }

        return "Recurso dado de baja exitosamente. Se eliminaron " + serviciosDelRecurso.size() +
                " servicios y se cancelaron " + turnosAfectados.size() + " turnos pendientes.";
    }
}