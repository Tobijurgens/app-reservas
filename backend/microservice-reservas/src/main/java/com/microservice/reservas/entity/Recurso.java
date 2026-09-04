package com.microservice.reservas.entity;

import com.microservice.reservas.enums.EstadoAsociacion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "recursos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Recurso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    @Column(columnDefinition = "boolean default true")
    private Boolean activo = true;

    @Enumerated(EnumType.STRING)
    private EstadoAsociacion estadoAsociacion;

    private String tokenInvitacion;

    @OneToMany(mappedBy = "recurso", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HorarioRecurso> horarios;
}