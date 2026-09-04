package com.microservice.reservas.repository;

import com.microservice.reservas.entity.Recurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecursoRepository extends JpaRepository<Recurso, Long> {
    List<Recurso> findByActivoTrue();
    Optional<Recurso> findByIdAndActivoTrue(Long id);
    List<Recurso> findByNegocioIdAndActivoTrue(Long negocioId);
    List<Recurso> findByNegocioId(Long negocioId);
    List<Recurso> findByUsuarioEmailAndActivoTrue(String email);
    Optional<Recurso> findByTokenInvitacion(String tokenInvitacion);
}