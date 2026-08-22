package com.microservice.reservas.repository;

import com.microservice.reservas.entity.Usuario;
import com.microservice.reservas.enums.EstadoCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByEmailAndEstadoCuenta(String email, EstadoCuenta estadoCuenta);
    List<Usuario> findByEstadoCuenta(EstadoCuenta estadoCuenta);
    Optional<Usuario> findByIdAndEstadoCuenta(Long id, EstadoCuenta estadoCuenta);
}