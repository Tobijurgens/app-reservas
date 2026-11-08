package com.microservice.reservas.dto.response;

import com.microservice.reservas.dto.request.HorarioSemanalDTO;
import com.microservice.reservas.entity.Negocio;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class NegocioResponseDTO {
    private Long id;
    private String nombre;
    private String alias;
    private String direccion;
    private String telefono;
    private String emailContacto;
    private String descripcion;
    private boolean propietario;

    private List<HorarioSemanalDTO> horarios = new ArrayList<>();

    public NegocioResponseDTO(Negocio negocio, String emailLogueado) {
        this.id = negocio.getId();
        this.nombre = negocio.getNombre();
        this.alias = negocio.getAlias();
        this.direccion = negocio.getDireccion();
        this.telefono = negocio.getTelefono();
        this.emailContacto = negocio.getEmailContacto();
        this.descripcion = negocio.getDescripcion();

        if (negocio.getOwner() != null) {
            this.propietario = negocio.getOwner().getEmail().equals(emailLogueado);
        }

        if (negocio.getHorarios() != null) {
            this.horarios = negocio.getHorarios().stream().map(h -> {
                HorarioSemanalDTO dto = new HorarioSemanalDTO();
                dto.setDiaSemana(h.getDiaSemana());
                dto.setActivo(h.getAbierto());
                dto.setHoraInicio(h.getHoraApertura());
                dto.setHoraFin(h.getHoraCierre());
                dto.setHoraInicioDescanso(h.getHoraInicioDescanso());
                dto.setHoraFinDescanso(h.getHoraFinDescanso());
                return dto;
            }).collect(Collectors.toList());
        }
    }
}
