package com.microservice.reservas.repository;

import com.microservice.reservas.entity.TokenSeguridad;
import com.microservice.reservas.enums.TipoToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenSeguridadRepository extends JpaRepository<TokenSeguridad, Long> {
    Optional<TokenSeguridad> findByToken(String token);
    Optional<TokenSeguridad> findByUsuarioIdAndTipo(Long usuarioId, TipoToken tipo);
}