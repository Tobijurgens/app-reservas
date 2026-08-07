package com.microservice.reservas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "turnos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guarda la fecha y hora exacta del turno
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;

    // Puede ser "PENDIENTE", "CONFIRMADO" o "CANCELADO"
    private String estado;

    // RELACIONES CON LAS OTRAS TABLAS:

    // "Muchos turnos pueden ser del mismo cliente"
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Usuario cliente;

    // El turno ahora se asocia al Recurso (Empleado, Cancha, etc.)
    @ManyToOne
    @JoinColumn(name = "recurso_id")
    private Recurso recurso;

    @ManyToOne
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

}