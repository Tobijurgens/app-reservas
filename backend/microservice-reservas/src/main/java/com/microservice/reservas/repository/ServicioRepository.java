package com.microservice.reservas.repository;

import com.microservice.reservas.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    List<Servicio> findByActivoTrue();
    Optional<Servicio> findByIdAndActivoTrue(Long id);
    List<Servicio> findByRecursoIdAndActivoTrue(Long recursoId);
}