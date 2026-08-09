package com.microservice.reservas.repository;

import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.entity.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno,Long>{

    // 1. SOLAPAMIENTO CORREGIDO
    @Query("SELECT COUNT(t) > 0 FROM Turno t WHERE t.recurso.id = :recursoId " +
            "AND t.fechaHoraInicio < :fin AND t.fechaHoraFin > :inicio " +
            "AND t.estado != :estadoIgnorado")
    boolean existeSolapamiento(
            @Param("recursoId") Long recursoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estadoIgnorado") EstadoTurno estadoIgnorado);

    // 2. DISPONIBILIDAD
    @Query("SELECT t FROM Turno t WHERE t.recurso.id = :recursoId " +
            "AND t.fechaHoraInicio BETWEEN :inicio AND :fin " +
            "AND t.estado != :estadoIgnorado")
    List<Turno> findTurnosOcupados(
            @Param("recursoId") Long recursoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estadoIgnorado") EstadoTurno estadoIgnorado);

    // Actualizá también las firmas de los métodos que usaban List<String> estados
    List<Turno> findByClienteId(Long clienteId);
    List<Turno> findByRecursoIdAndEstadoIn(Long recursoId, List<EstadoTurno> estados);
    List<Turno> findByServicioIdAndEstadoIn(Long servicioId, List<EstadoTurno> estados);
    List<Turno> findByClienteIdAndEstadoIn(Long clienteId, List<EstadoTurno> estados);
    // Búsqueda de todos los turnos asignados a un recurso específico
    List<Turno> findByRecursoId(Long recursoId);
}