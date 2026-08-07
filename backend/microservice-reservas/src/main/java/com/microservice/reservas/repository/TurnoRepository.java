package com.microservice.reservas.repository;

import com.microservice.reservas.entity.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno,Long>{

    // 1. SOLAPAMIENTO CORREGIDO (ahora ignora los cancelados)
    @Query("SELECT COUNT(t) > 0 FROM Turno t WHERE t.recurso.id = :recursoId " +
            "AND t.fechaHoraInicio < :fin AND t.fechaHoraFin > :inicio " +
            "AND t.estado != 'CANCELADO'")
    boolean existeSolapamiento(
            @Param("recursoId") Long recursoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    // Búsqueda por cliente (este queda igual porque es cortito)
    List<Turno> findByClienteId(Long clienteId);

    // 2. DISPONIBILIDAD CON NOMBRE LIMPIO
    @Query("SELECT t FROM Turno t WHERE t.recurso.id = :recursoId " +
            "AND t.fechaHoraInicio BETWEEN :inicio AND :fin " +
            "AND t.estado != 'CANCELADO'")
    List<Turno> findTurnosOcupados(
            @Param("recursoId") Long recursoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    // Busca turnos de un recurso filtrando por una lista de estados
    List<Turno> findByRecursoIdAndEstadoIn(Long recursoId, List<String> estados);
    // Busca turnos pendientes de un servicio específico
    List<Turno> findByServicioIdAndEstadoIn(Long servicioId, List<String> estados);
    // Busca turnos pendientes de un cliente específico
    List<Turno> findByClienteIdAndEstadoIn(Long clienteId, List<String> estados);
}