package com.microservice.reservas.repository;

import com.microservice.reservas.entity.Recurso;
import com.microservice.reservas.enums.EstadoAsociacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
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
    boolean existsByNegocioIdAndUsuarioEmailAndEstadoAsociacionAndActivoTrue(Long negocioId, String email, EstadoAsociacion estado);
    Optional<Recurso> findByTokenInvitacion(String tokenInvitacion);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Recurso r WHERE r.tokenInvitacion = :token")
    Optional<Recurso> findByTokenInvitacionParaActualizar(@Param("token") String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Recurso r WHERE r.id = :id AND r.activo = true")
    Optional<Recurso> findByIdActivoParaActualizar(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Recurso r SET r.activo = false WHERE r.negocio.id = :negocioId")
    int desactivarPorNegocioId(@Param("negocioId") Long negocioId);
}
