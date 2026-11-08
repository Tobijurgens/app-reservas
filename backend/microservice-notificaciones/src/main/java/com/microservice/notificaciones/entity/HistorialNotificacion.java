package com.microservice.notificaciones.entity;

import com.microservice.notificaciones.enums.EstadoNotificacion;
import com.microservice.notificaciones.enums.TipoNotificacion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_notificaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String emailDestinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoNotificacion estado;

    @Column(nullable = false)
    private LocalDateTime fechaEnvio;

    @Column(columnDefinition = "TEXT")
    private String detalleError;

    @Column(columnDefinition = "TEXT")
    private String titulo;
    @Column(columnDefinition = "TEXT")
    private String mensaje;
    private boolean leida = false;
    private boolean soloInApp = false;
}
