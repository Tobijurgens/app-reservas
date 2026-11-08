package com.microservice.notificaciones.controller;

import com.microservice.notificaciones.dto.NotificacionResponseDTO;
import com.microservice.notificaciones.repository.HistorialNotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final HistorialNotificacionRepository repository;

    @GetMapping
    public ResponseEntity<List<NotificacionResponseDTO>> obtenerMisNotificaciones(Principal principal) {
        var notificaciones = repository.findByEmailDestinatarioOrderByFechaEnvioDesc(principal.getName())
                .stream().map(NotificacionResponseDTO::new).toList();
        return ResponseEntity.ok(notificaciones);
    }

    @GetMapping("/no-leidas/count")
    public ResponseEntity<Long> contarNoLeidas(Principal principal) {
        return ResponseEntity.ok(repository.countByEmailDestinatarioAndLeidaFalse(principal.getName()));
    }

    @Transactional
    @PutMapping("/{id}/leer")
    public ResponseEntity<Void> marcarComoLeida(@PathVariable Long id, Principal principal) {
        var notificacion = repository.findByIdAndEmailDestinatario(id, principal.getName());
        if (notificacion.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var propia = notificacion.get();
        propia.setLeida(true);
        repository.save(propia);
        return ResponseEntity.ok().build();
    }

    @Transactional
    @PutMapping("/leer-todas")
    public ResponseEntity<Void> marcarTodasComoLeidas(Principal principal) {
        repository.marcarTodasComoLeidas(principal.getName());
        return ResponseEntity.ok().build();
    }
}
