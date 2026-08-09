package com.microservice.reservas.service;

import com.microservice.reservas.dto.request.NegocioDTO;
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
public class NegocioService {

    private final NegocioRepository negocioRepository;
    private final UsuarioRepository usuarioRepository;
    private final RecursoRepository recursoRepository;
    private final ServicioRepository servicioRepository;
    private final TurnoRepository turnoRepository;

    public Negocio crearNegocio(NegocioDTO negocioDto, String emailOwner) {
        Usuario owner = usuarioRepository.findByEmailAndActivoTrue(emailOwner)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado o inactivo."));

        Negocio negocio = new Negocio();
        negocio.setNombre(negocioDto.getNombre());
        negocio.setDireccion(negocioDto.getDireccion());
        negocio.setTelefono(negocioDto.getTelefono());
        negocio.setOwner(owner);
        negocio.setActivo(true);

        return negocioRepository.save(negocio);
    }

    public Negocio actualizarNegocio(Long id, NegocioDTO negocioDto, String emailLogueado) {
        Negocio negocio = negocioRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado o inactivo."));

        // CANDADO: Validar que el que edita es el dueño
        if (!negocio.getOwner().getEmail().equals(emailLogueado)) {
            throw new RuntimeException("Acceso denegado: No sos el dueño de este negocio.");
        }

        negocio.setNombre(negocioDto.getNombre());
        negocio.setDireccion(negocioDto.getDireccion());
        negocio.setTelefono(negocioDto.getTelefono());

        return negocioRepository.save(negocio);
    }

    public List<Negocio> obtenerTodos() {
        return negocioRepository.findByActivoTrue();
    }

    public Optional<Negocio> obtenerPorId(Long id) {
        return negocioRepository.findByIdAndActivoTrue(id);
    }

    public String eliminarNegocio(Long id, String emailLogueado) {
        Negocio negocio = negocioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El negocio no existe."));

        if (!negocio.getActivo()) {
            throw new RuntimeException("Ya estaba eliminado.");
        }

        // CANDADO: Solo el dueño puede eliminarlo
        if (!negocio.getOwner().getEmail().equals(emailLogueado)) {
            throw new RuntimeException("Acceso denegado: Solo el dueño puede eliminar este negocio.");
        }

        negocio.setActivo(false);
        negocioRepository.save(negocio);

        int recursosBorrados = 0;
        int serviciosBorrados = 0;
        int turnosCancelados = 0;
        List<EstadoTurno> estadosACancelar = Arrays.asList(EstadoTurno.PENDIENTE, EstadoTurno.CONFIRMADO);

        List<Recurso> recursos = recursoRepository.findByNegocioIdAndActivoTrue(id);
        for (Recurso recurso : recursos) {
            recurso.setActivo(false);
            recursoRepository.save(recurso);
            recursosBorrados++;

            List<Servicio> servicios = servicioRepository.findByRecursoIdAndActivoTrue(recurso.getId());
            for (Servicio servicio : servicios) {
                servicio.setActivo(false);
                servicioRepository.save(servicio);
                serviciosBorrados++;
            }

            List<Turno> turnos = turnoRepository.findByRecursoIdAndEstadoIn(recurso.getId(), estadosACancelar);
            for (Turno turno : turnos) {
                turno.setEstado(EstadoTurno.CANCELADO);
                turnoRepository.save(turno);
                turnosCancelados++;
            }
        }

        return "Negocio dado de baja exitosamente. Se eliminaron " + recursosBorrados +
                " recursos, " + serviciosBorrados + " servicios y se cancelaron " +
                turnosCancelados + " turnos pendientes.";
    }
}