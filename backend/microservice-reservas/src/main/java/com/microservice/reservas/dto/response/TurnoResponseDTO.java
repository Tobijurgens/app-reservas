package com.microservice.reservas.dto.response;

import com.microservice.reservas.entity.Turno;
import com.microservice.reservas.enums.EstadoTurno;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class TurnoResponseDTO {
    private Long id;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private EstadoTurno estado;

    private Long servicioId;
    private String nombreServicio;
    private Double precio;

    private String nombreRecurso;

    private String nombreNegocio;
    private String direccionNegocio;
    private Long negocioId;
    private String aliasNegocio;

    private String emailCliente;
    private String nombreCliente;
    private String apellidoCliente;
    private String telefonoCliente;


    public TurnoResponseDTO(Turno turno) {
        this.id = turno.getId();
        this.fechaHoraInicio = turno.getFechaHoraInicio();
        this.fechaHoraFin = turno.getFechaHoraFin();
        this.estado = turno.getEstado();

        if (turno.getCliente() != null) {
            this.emailCliente = turno.getCliente().getEmail();
            this.nombreCliente = turno.getCliente().getNombre();
            this.apellidoCliente = turno.getCliente().getApellido();
            this.telefonoCliente = turno.getCliente().getTelefono();
        }

        if (turno.getServicio() != null) {
            this.servicioId = turno.getServicio().getId();
            this.nombreServicio = turno.getServicio().getNombre();
            this.precio = turno.getServicio().getPrecio();

            if (turno.getServicio().getRecurso() != null) {
                this.nombreRecurso = turno.getServicio().getRecurso().getNombre();

                if (turno.getServicio().getRecurso().getNegocio() != null) {
                    this.nombreNegocio = turno.getServicio().getRecurso().getNegocio().getNombre();
                    this.direccionNegocio = turno.getServicio().getRecurso().getNegocio().getDireccion();
                    this.negocioId = turno.getServicio().getRecurso().getNegocio().getId();
                    this.aliasNegocio = turno.getServicio().getRecurso().getNegocio().getAlias();
                }
            }
        }
    }
}