package com.microservice.reservas.service;

import com.microservice.reservas.dto.request.TurnoDTO;
import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.entity.Recurso;
import com.microservice.reservas.entity.Servicio;
import com.microservice.reservas.entity.Turno;
import com.microservice.reservas.entity.Usuario;
import com.microservice.reservas.repository.RecursoRepository;
import com.microservice.reservas.repository.ServicioRepository;
import com.microservice.reservas.repository.TurnoRepository;
import com.microservice.reservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private final TurnoRepository turnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RecursoRepository recursoRepository;
    private final ServicioRepository servicioRepository;

    public Turno crearTurno(TurnoDTO turnoDto, String emailCliente) {
        Usuario cliente = usuarioRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (!cliente.getActivo()) {
            throw new RuntimeException("El cliente está inactivo");
        }

        // 1. Buscamos el servicio (ya no pedimos el idRecurso en el DTO)
        Servicio servicio = servicioRepository.findByIdAndActivoTrue(turnoDto.getIdServicio())
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado o inactivo"));

        // 2. Extraemos el recurso directamente de la base de datos
        Recurso recurso = servicio.getRecurso();

        if (!recurso.getActivo()) {
            throw new RuntimeException("El recurso asociado a este servicio está inactivo");
        }

        LocalDateTime inicio = turnoDto.getFechaHoraInicio();
        LocalDateTime fin = inicio.plusMinutes(servicio.getDuracionMinutos());

        boolean ocupado = turnoRepository.existeSolapamiento(recurso.getId(), inicio, fin, EstadoTurno.CANCELADO);
        if (ocupado) {
            throw new RuntimeException("El recurso ya tiene un turno reservado en este horario");
        }

        Turno turno = new Turno();
        turno.setFechaHoraInicio(inicio);
        turno.setFechaHoraFin(fin);
        turno.setEstado(EstadoTurno.PENDIENTE);
        turno.setCliente(cliente);
        turno.setRecurso(recurso);
        turno.setServicio(servicio);

        return turnoRepository.save(turno);
    }

    public Turno actualizarEstado(Long id, EstadoTurno nuevoEstado) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El turno especificado no existe."));

        turno.setEstado(nuevoEstado);
        return turnoRepository.save(turno);
    }

    public List<Turno> obtenerTodos() {
        return turnoRepository.findAll();
    }

    public Optional<Turno> obtenerPorId(Long id) {
        return turnoRepository.findById(id);
    }

    public List<Turno> obtenerMisTurnos(String emailCliente) {
        Usuario cliente = usuarioRepository.findByEmailAndActivoTrue(emailCliente)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado o inactivo."));

        return turnoRepository.findByClienteId(cliente.getId());
    }

    public List<Turno> obtenerAgendaRecurso(Long recursoId, String emailLogueado) {
        Recurso recurso = recursoRepository.findByIdAndActivoTrue(recursoId)
                .orElseThrow(() -> new RuntimeException("El recurso no existe."));

        String emailOwner = recurso.getNegocio().getOwner().getEmail();
        boolean esElOwner = emailOwner.equals(emailLogueado);

        boolean esElRecurso = recurso.getUsuario() != null && recurso.getUsuario().getEmail().equals(emailLogueado);

        if (!esElOwner && !esElRecurso) {
            throw new RuntimeException("Acceso denegado: No tenés permisos para ver la agenda de este recurso.");
        }

        return turnoRepository.findByRecursoId(recursoId);
    }

    public List<Turno> obtenerTurnosOcupados(Long recursoId, LocalDateTime inicioDia, LocalDateTime finDia) {
        return turnoRepository.findTurnosOcupados(recursoId, inicioDia, finDia, EstadoTurno.CANCELADO);
    }

    public String cancelarTurno(Long id) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El turno no existe."));

        if (turno.getEstado() == EstadoTurno.CANCELADO) {
            throw new RuntimeException("El turno ya se encontraba cancelado.");
        }

        turno.setEstado(EstadoTurno.CANCELADO);
        turnoRepository.save(turno);
        return "Turno cancelado correctamente.";
    }
}