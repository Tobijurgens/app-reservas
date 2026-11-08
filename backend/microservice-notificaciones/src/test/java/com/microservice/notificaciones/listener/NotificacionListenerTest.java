package com.microservice.notificaciones.listener;

import com.microservice.notificaciones.enums.TipoEventoRecibido;
import com.microservice.notificaciones.service.RecepcionEventoService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class NotificacionListenerTest {
    @ParameterizedTest
    @EnumSource(TipoEventoRecibido.class)
    void todosLosFlujosEntreganElMensajeCrudoALaPersistencia(TipoEventoRecibido tipo) {
        var recepcion = mock(RecepcionEventoService.class);
        var listener = new NotificacionListener(recepcion);
        Message mensaje = new Message(new byte[] {0, 1, 2}, new MessageProperties());
        enviar(listener, mensaje, tipo);
        verify(recepcion).registrar(mensaje, tipo);
    }

    @ParameterizedTest
    @EnumSource(TipoEventoRecibido.class)
    void falloDePersistenciaNoSeOcultaComoRecepcionExitosa(TipoEventoRecibido tipo) {
        var recepcion = mock(RecepcionEventoService.class);
        var listener = new NotificacionListener(recepcion);
        Message mensaje = new Message(new byte[0], new MessageProperties());
        doThrow(new IllegalStateException("DB temporalmente inaccesible")).when(recepcion).registrar(mensaje, tipo);
        assertThrows(IllegalStateException.class, () -> enviar(listener, mensaje, tipo));
    }

    private void enviar(NotificacionListener listener, Message mensaje, TipoEventoRecibido tipo) {
        switch (tipo) {
            case REGISTRO -> listener.procesarRegistroUsuario(mensaje);
            case INVITACION -> listener.procesarInvitacionEmpleado(mensaje);
            case TURNO -> listener.procesarTurno(mensaje);
            case AVISO -> listener.procesarAvisoInterno(mensaje);
            case ELIMINACION -> listener.procesarEliminacionUsuario(mensaje);
        }
    }
}
