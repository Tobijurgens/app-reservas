package com.microservice.reservas.service;

import com.microservice.reservas.dto.request.ServicioDTO;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.repository.*;
import com.microservice.reservas.security.PermisosRecurso;
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

        PermisosRecurso.validarGestion(recurso, emailLogueado);

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

        PermisosRecurso.validarGestion(servicio.getRecurso(), emailLogueado);

        servicio.setNombre(servicioDto.getNombre());
        servicio.setDescripcion(servicioDto.getDescripcion());
        servicio.setPrecio(servicioDto.getPrecio());
        servicio.setDuracionMinutos(servicioDto.getDuracionMinutos());

        return servicioRepository.save(servicio);
    }

    public List<Servicio> obtenerTodos() {
        return servicioRepository.findByActivoTrue().stream().filter(this::esPublicable).toList();
    }

    public Optional<Servicio> obtenerPorId(Long id) {
        return servicioRepository.findByIdAndActivoTrue(id).filter(this::esPublicable);
    }

    public List<Servicio> obtenerPorRecurso(Long recursoId) {
        Recurso recurso = recursoRepository.findByIdAndActivoTrue(recursoId)
                .orElseThrow(() -> new RuntimeException("El recurso no existe o está inactivo."));
        if (!Boolean.TRUE.equals(recurso.getNegocio().getActivo())) {
            throw new RuntimeException("El negocio no existe o está inactivo.");
        }
        return servicioRepository.findByRecursoIdAndActivoTrue(recursoId);
    }

    private boolean esPublicable(Servicio servicio) {
        return Boolean.TRUE.equals(servicio.getRecurso().getActivo())
                && Boolean.TRUE.equals(servicio.getRecurso().getNegocio().getActivo());
    }

    @Transactional
    public String eliminarServicio(Long id, String emailLogueado) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El servicio no existe."));

        if (!servicio.getActivo()) {
            throw new RuntimeException("Ya estaba eliminado.");
        }

        PermisosRecurso.validarGestion(servicio.getRecurso(), emailLogueado);

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

}
