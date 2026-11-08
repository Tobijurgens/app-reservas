package com.microservice.notificaciones.repository;

import com.microservice.notificaciones.entity.HistorialNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface HistorialNotificacionRepository extends JpaRepository<HistorialNotificacion, Long> {
    List<HistorialNotificacion> findByEmailDestinatarioOrderByFechaEnvioDesc(String emailDestinatario);
    long countByEmailDestinatarioAndLeidaFalse(String emailDestinatario);
    Optional<HistorialNotificacion> findByIdAndEmailDestinatario(Long id, String emailDestinatario);

    @Modifying
    @Query("UPDATE HistorialNotificacion h SET h.leida = true WHERE h.emailDestinatario = :email")
    void marcarTodasComoLeidas(String email);

    @Modifying
    @Query("DELETE FROM HistorialNotificacion h WHERE h.emailDestinatario = :email")
    void deleteAllByEmailDestinatario(String email);
}
