package com.microservice.reservas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "horarios_recurso", uniqueConstraints = {@UniqueConstraint(columnNames = {"recurso_id", "dia_semana"})})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HorarioRecurso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek diaSemana;

    @Column(nullable = false)
    private Boolean activo = true;

    private LocalTime horaInicio;
    private LocalTime horaFin;
    private LocalTime horaInicioDescanso;
    private LocalTime horaFinDescanso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurso_id")
    private Recurso recurso;
}