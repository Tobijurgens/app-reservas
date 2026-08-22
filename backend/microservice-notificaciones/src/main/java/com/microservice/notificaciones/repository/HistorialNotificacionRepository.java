package com.microservice.notificaciones.repository;

import com.microservice.notificaciones.entity.HistorialNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistorialNotificacionRepository extends JpaRepository<HistorialNotificacion, Long> {
}