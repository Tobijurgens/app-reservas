package com.microservice.reservas.repository;

import com.microservice.reservas.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Para buscar a nivel general (usado en el POST para evitar choques)
    Optional<Usuario> findByEmail(String email);

    // Para el login y búsquedas normales (ignora los borrados)
    Optional<Usuario> findByEmailAndActivoTrue(String email);

    List<Usuario> findByActivoTrue();
    Optional<Usuario> findByIdAndActivoTrue(Long id);
}