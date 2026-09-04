package com.microservice.reservas.service;

import com.microservice.reservas.dto.request.TurnoDTO;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.enums.EstadoCuenta;
import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurnoServiceTest {

    @Mock
    private TurnoRepository turnoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ServicioRepository servicioRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private TurnoService turnoService;

    private Usuario clienteMock;
    private Servicio servicioMock;
    private TurnoDTO turnoDtoMock;

    @BeforeEach
    void setUp() {
        clienteMock = new Usuario();
        clienteMock.setEmail("cliente@test.com");
        clienteMock.setEstadoCuenta(EstadoCuenta.ACTIVO);
        clienteMock.setNombre("Juan");
        clienteMock.setApellido("Pérez");

        Negocio negocioMock = new Negocio();
        negocioMock.setNombre("Barbería Test");

        Recurso recursoMock = new Recurso();
        recursoMock.setId(1L);
        recursoMock.setActivo(true);
        recursoMock.setNombre("Sillón 1");
        recursoMock.setNegocio(negocioMock);

        servicioMock = new Servicio();
        servicioMock.setId(1L);
        servicioMock.setDuracionMinutos(30);
        servicioMock.setRecurso(recursoMock);
        servicioMock.setNombre("Corte Clásico");

        turnoDtoMock = new TurnoDTO();
        turnoDtoMock.setIdServicio(1L);
        turnoDtoMock.setFechaHoraInicio(LocalDateTime.now().plusDays(1));
    }

    @Test
    void crearTurno_Exito() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(clienteMock));
        when(servicioRepository.findByIdAndActivoTrue(anyLong())).thenReturn(Optional.of(servicioMock));
        when(turnoRepository.existeSolapamiento(anyLong(), any(), any(), any())).thenReturn(false);

        when(turnoRepository.save(any(Turno.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Turno resultado = turnoService.crearTurno(turnoDtoMock, "cliente@test.com");

        assertNotNull(resultado);
        assertEquals(EstadoTurno.CONFIRMADO, resultado.getEstado());

        verify(turnoRepository, times(1)).save(any(Turno.class));
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void crearTurno_FallaPorSolapamiento() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(clienteMock));
        when(servicioRepository.findByIdAndActivoTrue(anyLong())).thenReturn(Optional.of(servicioMock));
        when(turnoRepository.existeSolapamiento(anyLong(), any(), any(), any())).thenReturn(true);

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> turnoService.crearTurno(turnoDtoMock, "cliente@test.com"));

        assertEquals("El horario seleccionado ya no se encuentra disponible. Por favor, elegí otro.", excepcion.getMessage());

        verify(turnoRepository, never()).save(any(Turno.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void crearTurno_FallaPorFechaEnElPasado() {
        turnoDtoMock.setFechaHoraInicio(LocalDateTime.now().minusDays(1));

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> turnoService.crearTurno(turnoDtoMock, "cliente@test.com"));

        assertEquals("No se pueden reservar turnos en el pasado.", excepcion.getMessage());
        verify(usuarioRepository, never()).findByEmail(anyString());
    }
}