package com.microservice.reservas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "horarios_negocio", uniqueConstraints = {@UniqueConstraint(columnNames = {"negocio_id", "dia_semana"})})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HorarioNegocio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek diaSemana;

    @Column(nullable = false)
    private Boolean abierto = true;

    private LocalTime horaApertura;
    private LocalTime horaCierre;
    private LocalTime horaInicioDescanso;
    private LocalTime horaFinDescanso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;
}