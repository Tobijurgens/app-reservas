package com.microservice.notificaciones.controller;

import com.microservice.notificaciones.entity.HistorialNotificacion;
import com.microservice.notificaciones.repository.HistorialNotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final HistorialNotificacionRepository repository;

    @GetMapping
    public ResponseEntity<List<HistorialNotificacion>> obtenerMisNotificaciones(@RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(repository.findByEmailDestinatarioOrderByFechaEnvioDesc(email));
    }

    @GetMapping("/no-leidas/count")
    public ResponseEntity<Long> contarNoLeidas(@RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(repository.countByEmailDestinatarioAndLeidaFalse(email));
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<Void> marcarComoLeida(@PathVariable Long id, @RequestHeader("X-User-Email") String email) {
        repository.findById(id).ifPresent(notif -> {
            if (notif.getEmailDestinatario().equals(email)) {
                notif.setLeida(true);
                repository.save(notif);
            }
        });
        return ResponseEntity.ok().build();
    }

    @Transactional
    @PutMapping("/leer-todas")
    public ResponseEntity<Void> marcarTodasComoLeidas(@RequestHeader("X-User-Email") String email) {
        repository.marcarTodasComoLeidas(email);
        return ResponseEntity.ok().build();
    }
}