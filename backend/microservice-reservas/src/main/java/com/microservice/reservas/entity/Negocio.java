package com.microservice.reservas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "negocios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Negocio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @Column(nullable = false, unique = true, length = 100)
    private String alias;

    private String direccion;
    private String telefono;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Usuario owner;

    @Column(length = 500)
    private String descripcion;

    private String emailContacto;

    @Column(columnDefinition = "boolean default true")
    private Boolean activo = true;

    @OneToMany(mappedBy = "negocio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HorarioNegocio> horarios;
}