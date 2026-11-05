package com.microservice.reservas.repository;

import com.microservice.reservas.entity.Recurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecursoRepository extends JpaRepository<Recurso, Long> {
    List<Recurso> findByActivoTrue();
    Optional<Recurso> findByIdAndActivoTrue(Long id);
    List<Recurso> findByNegocioIdAndActivoTrueOrderByNombreAsc(Long negocioId);
    List<Recurso> findByNegocioIdOrderByNombreAsc(Long negocioId);
    List<Recurso> findByUsuarioEmailAndActivoTrueOrderByNombreAsc(String email);
    Optional<Recurso> findByTokenInvitacion(String tokenInvitacion);
    @Modifying
    @Query("UPDATE Recurso r SET r.activo = false WHERE r.negocio.id = :negocioId")
    int desactivarPorNegocioId(@Param("negocioId") Long negocioId);
}