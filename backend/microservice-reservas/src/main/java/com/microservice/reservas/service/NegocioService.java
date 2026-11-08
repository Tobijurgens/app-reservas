package com.microservice.reservas.service;

import com.microservice.reservas.dto.request.HorarioSemanalDTO;
import com.microservice.reservas.dto.request.NegocioDTO;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.enums.EstadoCuenta;
import com.microservice.reservas.enums.EstadoAsociacion;
import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.repository.*;
import com.microservice.reservas.util.ValidacionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    @Transactional
    public Negocio crearNegocio(NegocioDTO negocioDto, String emailOwner) {
        String aliasLimpio = negocioDto.getAlias().toLowerCase().trim();
        if (negocioRepository.existsByAliasAndActivoTrue(aliasLimpio)) {
            throw new RuntimeException("Este link ya está en uso. Por favor, elegí otro alias.");
        }

        Usuario owner = usuarioRepository.findByEmailAndEstadoCuenta(emailOwner, EstadoCuenta.ACTIVO)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado o inactivo."));

        ValidacionUtil.validarSemanaCompleta(negocioDto.getHorarios());

        Negocio negocio = new Negocio();
        negocio.setAlias(aliasLimpio);
        negocio.setOwner(owner);
        negocio.setActivo(true);
        negocio.setHorarios(new ArrayList<>());

        mapearCamposComunes(negocio, negocioDto);

        return negocioRepository.save(negocio);
    }

    @Transactional
    public Negocio actualizarNegocio(Long id, NegocioDTO negocioDto, String emailLogueado) {
        Negocio negocio = negocioRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado o inactivo."));

        if (!negocio.getOwner().getEmail().equals(emailLogueado)) {
            throw new RuntimeException("Acceso denegado: No sos el dueño de este negocio.");
        }

        String nuevoAlias = negocioDto.getAlias().toLowerCase().trim();
        if (!negocio.getAlias().equals(nuevoAlias) && negocioRepository.existsByAliasAndActivoTrue(nuevoAlias)) {
            throw new RuntimeException("Este link ya está en uso. Por favor, elegí otro alias.");
        }

        ValidacionUtil.validarSemanaCompleta(negocioDto.getHorarios());

        negocio.setAlias(nuevoAlias);
        mapearCamposComunes(negocio, negocioDto);

        return negocioRepository.save(negocio);
    }

    public List<Negocio> obtenerMisNegocios(String emailLogueado) {
        Usuario owner = usuarioRepository.findByEmailAndEstadoCuenta(emailLogueado, EstadoCuenta.ACTIVO)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado o inactivo."));

        return negocioRepository.findByOwnerIdAndActivoTrue(owner.getId());
    }

    public List<Negocio> obtenerTodos() {
        return negocioRepository.findByActivoTrue();
    }

    public Optional<Negocio> obtenerPorId(Long id, String emailLogueado) {
        return negocioRepository.findByIdAndActivoTrue(id).map(negocio -> {
            if (!negocio.getOwner().getEmail().equals(emailLogueado)
                    && !recursoRepository.existsByNegocioIdAndUsuarioEmailAndEstadoAsociacionAndActivoTrue(
                            id, emailLogueado, EstadoAsociacion.ACTIVO)) {
                throw new AccessDeniedException("Acceso denegado: No tenés acceso administrativo a este negocio.");
            }
            return negocio;
        });
    }

    public Optional<Negocio> obtenerPorAlias(String alias) {
        return negocioRepository.findByAliasAndActivoTrue(alias.toLowerCase().trim());
    }

    @Transactional
    public String eliminarNegocio(Long id, String emailLogueado) {
        Negocio negocio = negocioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El negocio no existe."));

        if (!negocio.getActivo()) {
            throw new RuntimeException("Ya estaba eliminado.");
        }

        if (!negocio.getOwner().getEmail().equals(emailLogueado)) {
            throw new RuntimeException("Acceso denegado: Solo el dueño puede eliminar este negocio.");
        }

        negocio.setActivo(false);
        negocioRepository.save(negocio);

        int serviciosBorrados = servicioRepository.desactivarPorNegocioId(id);
        int recursosBorrados = recursoRepository.desactivarPorNegocioId(id);

        List<EstadoTurno> estadosACancelar = List.of(EstadoTurno.CONFIRMADO);
        int turnosCancelados = turnoRepository.actualizarEstadoPorNegocioId(id, EstadoTurno.CANCELADO, estadosACancelar);

        return "Negocio dado de baja exitosamente. Se eliminaron " + recursosBorrados +
                " recursos, " + serviciosBorrados + " servicios y se cancelaron " +
                turnosCancelados + " turnos pendientes.";
    }

    private void mapearCamposComunes(Negocio negocio, NegocioDTO dto) {
        negocio.setNombre(dto.getNombre());
        negocio.setDireccion(dto.getDireccion());
        negocio.setTelefono(dto.getTelefono());
        negocio.setDescripcion(dto.getDescripcion());
        negocio.setEmailContacto(dto.getEmailContacto());

        if (negocio.getHorarios() == null) {
            negocio.setHorarios(new ArrayList<>());
        }

        if (dto.getHorarios() != null) {
            for (HorarioSemanalDTO hDto : dto.getHorarios()) {
                HorarioNegocio existente = negocio.getHorarios().stream()
                        .filter(h -> h.getDiaSemana() == hDto.getDiaSemana())
                        .findFirst()
                        .orElse(null);

                if (existente != null) {
                    actualizarHorarioNegocio(existente, hDto);
                } else {
                    negocio.getHorarios().add(crearHorarioNegocio(hDto, negocio));
                }
            }
        }
    }

    private void actualizarHorarioNegocio(HorarioNegocio existente, HorarioSemanalDTO hDto) {
        existente.setAbierto(hDto.getActivo());
        existente.setHoraApertura(hDto.getHoraInicio());
        existente.setHoraCierre(hDto.getHoraFin());
        existente.setHoraInicioDescanso(hDto.getHoraInicioDescanso());
        existente.setHoraFinDescanso(hDto.getHoraFinDescanso());
    }

    private HorarioNegocio crearHorarioNegocio(HorarioSemanalDTO hDto, Negocio negocio) {
        HorarioNegocio nuevo = new HorarioNegocio();
        nuevo.setDiaSemana(hDto.getDiaSemana());
        nuevo.setAbierto(hDto.getActivo());
        nuevo.setHoraApertura(hDto.getHoraInicio());
        nuevo.setHoraCierre(hDto.getHoraFin());
        nuevo.setHoraInicioDescanso(hDto.getHoraInicioDescanso());
        nuevo.setHoraFinDescanso(hDto.getHoraFinDescanso());
        nuevo.setNegocio(negocio);
        return nuevo;
    }
}
