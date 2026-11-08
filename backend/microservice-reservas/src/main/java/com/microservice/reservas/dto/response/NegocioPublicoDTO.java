package com.microservice.reservas.dto.response;

import com.microservice.reservas.dto.request.HorarioSemanalDTO;
import com.microservice.reservas.entity.Negocio;

import java.util.List;

public record NegocioPublicoDTO(Long id, String nombre, String alias, String direccion,
                                String telefono, String emailContacto, String descripcion,
                                List<HorarioSemanalDTO> horarios) {
    public NegocioPublicoDTO(Negocio negocio) {
        this(negocio.getId(), negocio.getNombre(), negocio.getAlias(), negocio.getDireccion(),
                negocio.getTelefono(), negocio.getEmailContacto(), negocio.getDescripcion(),
                negocio.getHorarios().stream().map(h -> {
                    var dto = new HorarioSemanalDTO();
                    dto.setDiaSemana(h.getDiaSemana());
                    dto.setActivo(h.getAbierto());
                    dto.setHoraInicio(h.getHoraApertura());
                    dto.setHoraFin(h.getHoraCierre());
                    dto.setHoraInicioDescanso(h.getHoraInicioDescanso());
                    dto.setHoraFinDescanso(h.getHoraFinDescanso());
                    return dto;
                }).toList());
    }
}
