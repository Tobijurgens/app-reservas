package com.microservice.reservas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recursos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Recurso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre; // <-- Nuevo campo (Ej: "Cancha 1" o "Dr. Perez")
    private String descripcion;

    // Un recurso ES un usuario del sistema (el empleado, barbero, cancha, etc.)
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // Un recurso pertenece obligatoriamente a un Negocio
    @ManyToOne
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    @Column(columnDefinition = "boolean default true")
    private Boolean activo = true;
}