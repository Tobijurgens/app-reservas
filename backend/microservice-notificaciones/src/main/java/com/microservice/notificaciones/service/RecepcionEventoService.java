package com.microservice.notificaciones.service;

import com.microservice.notificaciones.enums.TipoEventoRecibido;
import com.microservice.notificaciones.repository.EventoRecibidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecepcionEventoService {
    private final EventoRecibidoRepository repository;

    @Transactional
    public void registrar(Message mensaje, TipoEventoRecibido tipo) {
        String id = IdentidadEvento.obtener(mensaje, tipo);
        int insertados = repository.insertarSiNoExiste(id, tipo.name(), mensaje.getBody());
        if (insertados == 0) log.info("Evento duplicado id={} tipo={}", id, tipo);
        else if (id.startsWith("legacy:")) log.warn("Evento recibido sin identificador: huella persistente id={} tipo={}", id, tipo);
        else log.info("Evento recibido id={} tipo={}", id, tipo);
    }
}
