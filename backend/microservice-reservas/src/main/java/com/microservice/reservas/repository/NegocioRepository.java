package com.microservice.reservas.repository;

import com.microservice.reservas.entity.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NegocioRepository extends JpaRepository<Negocio, Long> {
    List<Negocio> findByActivoTrue();
    Optional<Negocio> findByIdAndActivoTrue(Long id);
}