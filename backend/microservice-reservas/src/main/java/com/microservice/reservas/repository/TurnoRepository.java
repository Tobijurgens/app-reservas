package com.microservice.reservas.repository;

import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.entity.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno,Long>{

    // 1. SOLAPAMIENTO CORREGIDO usando x1, x2, x3
    @Query("SELECT COUNT(t) > 0 FROM Turno t WHERE t.recurso.id = :recursoId " +
            "AND t.fechaHoraInicio < :x2 AND t.fechaHoraFin > :x1 " +
            "AND t.estado != :x3")
    boolean existeSolapamiento(
            @Param("recursoId") Long recursoId,
            @Param("x1") LocalDateTime x1,
            @Param("x2") LocalDateTime x2,
            @Param("x3") EstadoTurno x3);

    // 2. DISPONIBILIDAD
    @Query("SELECT t FROM Turno t WHERE t.recurso.id = :recursoId " +
            "AND t.fechaHoraInicio BETWEEN :inicio AND :fin " +
            "AND t.estado != :estadoIgnorado")
    List<Turno> findTurnosOcupados(
            @Param("recursoId") Long recursoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estadoIgnorado") EstadoTurno estadoIgnorado);

    List<Turno> findByClienteId(Long clienteId);
    List<Turno> findByRecursoIdAndEstadoIn(Long recursoId, List<EstadoTurno> estados);
    List<Turno> findByServicioIdAndEstadoIn(Long servicioId, List<EstadoTurno> estados);
    List<Turno> findByClienteIdAndEstadoIn(Long clienteId, List<EstadoTurno> estados);
    List<Turno> findByRecursoId(Long recursoId);

    @Query("SELECT t FROM Turno t WHERE t.servicio.recurso.negocio.id = :negocioId ORDER BY t.fechaHoraInicio DESC")
    Page<Turno> findByNegocioIdDesc(@Param("negocioId") Long negocioId, Pageable pageable);

    @Query("SELECT t FROM Turno t WHERE t.recurso.id = :recursoId ORDER BY t.fechaHoraInicio DESC")
    Page<Turno> findByRecursoIdDesc(@Param("recursoId") Long recursoId, Pageable pageable);

    @Query("SELECT t FROM Turno t WHERE t.servicio.recurso.negocio.id = :negocioId " +
            "AND t.fechaHoraInicio >= :x1 AND t.fechaHoraInicio <= :x2")
    List<Turno> findTurnosParaReporte(
            @Param("negocioId") Long negocioId,
            @Param("x1") LocalDateTime x1,
            @Param("x2") LocalDateTime x2);

    @Query("SELECT t FROM Turno t WHERE t.servicio.recurso.negocio.id = :negocioId " +
            "AND t.recurso.id = :recursoId " +
            "AND t.fechaHoraInicio >= :x1 AND t.fechaHoraInicio <= :x2")
    List<Turno> findTurnosParaReportePorRecurso(
            @Param("negocioId") Long negocioId,
            @Param("recursoId") Long recursoId,
            @Param("x1") LocalDateTime x1,
            @Param("x2") LocalDateTime x2);
}