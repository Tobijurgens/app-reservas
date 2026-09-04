package com.microservice.reservas.service;

import com.microservice.reservas.dto.request.ServicioDTO;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServicioService {

    private final ServicioRepository servicioRepository;
    private final RecursoRepository recursoRepository;
    private final TurnoRepository turnoRepository;

    @Transactional
    public Servicio crearServicio(ServicioDTO servicioDto, String emailLogueado) {
        Recurso recurso = recursoRepository.findByIdAndActivoTrue(servicioDto.getIdRecurso())
                .orElseThrow(() -> new RuntimeException("El recurso especificado no existe o está inactivo."));

        validarPermisos(recurso, emailLogueado);

        Servicio servicio = new Servicio();
        servicio.setNombre(servicioDto.getNombre());
        servicio.setDescripcion(servicioDto.getDescripcion());
        servicio.setPrecio(servicioDto.getPrecio());
        servicio.setDuracionMinutos(servicioDto.getDuracionMinutos());
        servicio.setRecurso(recurso);
        servicio.setActivo(true);

        return servicioRepository.save(servicio);
    }

    @Transactional
    public Servicio actualizarServicio(Long id, ServicioDTO servicioDto, String emailLogueado) {
        Servicio servicio = servicioRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RuntimeException("El servicio especificado no existe o está inactivo."));

        validarPermisos(servicio.getRecurso(), emailLogueado);

        servicio.setNombre(servicioDto.getNombre());
        servicio.setDescripcion(servicioDto.getDescripcion());
        servicio.setPrecio(servicioDto.getPrecio());
        servicio.setDuracionMinutos(servicioDto.getDuracionMinutos());

        return servicioRepository.save(servicio);
    }

    public List<Servicio> obtenerTodos() {
        return servicioRepository.findByActivoTrue();
    }

    public Optional<Servicio> obtenerPorId(Long id) {
        return servicioRepository.findByIdAndActivoTrue(id);
    }

    public List<Servicio> obtenerPorRecurso(Long recursoId) {
        return servicioRepository.findByRecursoIdAndActivoTrue(recursoId);
    }

    @Transactional
    public String eliminarServicio(Long id, String emailLogueado) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El servicio no existe."));

        if (!servicio.getActivo()) {
            throw new RuntimeException("Ya estaba eliminado.");
        }

        validarPermisos(servicio.getRecurso(), emailLogueado);

        servicio.setActivo(false);
        servicioRepository.save(servicio);

        List<EstadoTurno> estadosACancelar = List.of(EstadoTurno.CONFIRMADO);
        List<Turno> turnosAfectados = turnoRepository.findByServicioIdAndEstadoIn(id, estadosACancelar);

        for (Turno turno : turnosAfectados) {
            turno.setEstado(EstadoTurno.CANCELADO);
            turnoRepository.save(turno);
        }

        return "Servicio eliminado lógicamente. Se cancelaron " + turnosAfectados.size() + " turnos pendientes.";
    }

    private void validarPermisos(Recurso recurso, String emailLogueado) {
        String emailOwnerNegocio = recurso.getNegocio().getOwner().getEmail();
        boolean esElOwner = emailOwnerNegocio.equals(emailLogueado);
        boolean esElRecurso = recurso.getUsuario() != null && recurso.getUsuario().getEmail().equals(emailLogueado);

        if (!esElOwner && !esElRecurso) {
            throw new RuntimeException("Acceso denegado: Solo el dueño del negocio o el recurso asignado pueden gestionar servicios.");
        }
    }
}