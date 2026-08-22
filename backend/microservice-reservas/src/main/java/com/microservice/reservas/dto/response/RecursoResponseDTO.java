package com.microservice.reservas.dto.response;

import com.microservice.reservas.dto.request.HorarioSemanalDTO;
import com.microservice.reservas.entity.Recurso;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class RecursoResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private Long negocioId;
    private String nombreNegocio;
    private String emailUsuario;
    private Boolean activo;
    private List<HorarioSemanalDTO> horarios = new ArrayList<>();

    public RecursoResponseDTO(Recurso recurso) {
        this.id = recurso.getId();
        this.nombre = recurso.getNombre();
        this.descripcion = recurso.getDescripcion();
        this.activo = recurso.getActivo();

        if (recurso.getNegocio() != null) {
            this.negocioId = recurso.getNegocio().getId();
            this.nombreNegocio = recurso.getNegocio().getNombre();
        }
        if (recurso.getUsuario() != null) {
            this.emailUsuario = recurso.getUsuario().getEmail();
        }

        if (recurso.getHorarios() != null) {
            this.horarios = recurso.getHorarios().stream().map(h -> {
                HorarioSemanalDTO dto = new HorarioSemanalDTO();
                dto.setDiaSemana(h.getDiaSemana());
                dto.setActivo(h.getActivo());
                dto.setHoraInicio(h.getHoraInicio());
                dto.setHoraFin(h.getHoraFin());
                dto.setHoraInicioDescanso(h.getHoraInicioDescanso());
                dto.setHoraFinDescanso(h.getHoraFinDescanso());
                return dto;
            }).collect(Collectors.toList());
        }
    }
}