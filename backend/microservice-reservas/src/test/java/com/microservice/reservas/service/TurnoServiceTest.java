package com.microservice.reservas.service;

import com.microservice.reservas.dto.request.TurnoDTO;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.enums.EstadoCuenta;
import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.exception.TurnoException;
import com.microservice.reservas.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.microservice.reservas.service.OutboxService;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
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
    private RecursoRepository recursoRepository;
    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private TurnoService turnoService;

    private Usuario clienteMock;
    private Servicio servicioMock;
    private TurnoDTO turnoDtoMock;
    private Recurso recursoMock;
    private Negocio negocioMock;

    @BeforeEach
    void setUp() {
        clienteMock = new Usuario();
        clienteMock.setEmail("cliente@test.com");
        clienteMock.setEstadoCuenta(EstadoCuenta.ACTIVO);
        clienteMock.setNombre("Juan");
        clienteMock.setApellido("Pérez");

        negocioMock = new Negocio();
        negocioMock.setId(1L);
        negocioMock.setNombre("Barbería Test");
        Usuario ownerMock = new Usuario();
        ownerMock.setEmail("owner@test.com");
        negocioMock.setOwner(ownerMock);

        recursoMock = new Recurso();
        recursoMock.setId(1L);
        recursoMock.setActivo(true);
        recursoMock.setNombre("Sillón 1");
        recursoMock.setNegocio(negocioMock);
        recursoMock.setHorarios(Arrays.stream(DayOfWeek.values()).map(dia -> {
            HorarioRecurso h = new HorarioRecurso();
            h.setDiaSemana(dia);
            h.setHoraInicio(LocalTime.of(9, 0));
            h.setHoraFin(LocalTime.of(18, 0));
            return h;
        }).toList());
        negocioMock.setHorarios(Arrays.stream(DayOfWeek.values()).map(dia -> {
            HorarioNegocio h = new HorarioNegocio();
            h.setDiaSemana(dia);
            h.setHoraApertura(LocalTime.of(9, 0));
            h.setHoraCierre(LocalTime.of(18, 0));
            return h;
        }).toList());

        servicioMock = new Servicio();
        servicioMock.setId(1L);
        servicioMock.setDuracionMinutos(30);
        servicioMock.setRecurso(recursoMock);
        servicioMock.setNombre("Corte Clásico");

        turnoDtoMock = new TurnoDTO();
        turnoDtoMock.setIdServicio(1L);
        turnoDtoMock.setFechaHoraInicio(LocalDateTime.now().plusDays(1).with(LocalTime.of(10, 0)));
    }

    @Test
    void crearTurno_Exito() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(clienteMock));
        when(servicioRepository.findByIdAndActivoTrue(anyLong())).thenReturn(Optional.of(servicioMock));
        when(recursoRepository.findByIdActivoParaActualizar(1L)).thenReturn(Optional.of(recursoMock));
        when(turnoRepository.existeSolapamiento(anyLong(), any(), any(), any())).thenReturn(false);

        when(turnoRepository.save(any(Turno.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Turno resultado = turnoService.crearTurno(turnoDtoMock, "cliente@test.com");

        assertNotNull(resultado);
        assertEquals(EstadoTurno.CONFIRMADO, resultado.getEstado());
        assertEquals(turnoDtoMock.getFechaHoraInicio().plusMinutes(30), resultado.getFechaHoraFin());
        var orden = inOrder(recursoRepository, turnoRepository);
        orden.verify(recursoRepository).findByIdActivoParaActualizar(1L);
        orden.verify(turnoRepository).existeSolapamiento(eq(1L), any(), any(), eq(EstadoTurno.CONFIRMADO));
        orden.verify(turnoRepository).save(any(Turno.class));

        verify(turnoRepository, times(1)).save(any(Turno.class));
        verify(outboxService).encolar(eq("notificaciones.exchange"), eq("turno.cliente.routing.key"), any(Object.class));
        verify(outboxService).encolar(eq("notificaciones.exchange"), eq("avisos.internos.routing.key"), any(Object.class));
    }

    @Test
    void crearTurno_FallaPorSolapamiento() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(clienteMock));
        when(servicioRepository.findByIdAndActivoTrue(anyLong())).thenReturn(Optional.of(servicioMock));
        when(recursoRepository.findByIdActivoParaActualizar(1L)).thenReturn(Optional.of(recursoMock));
        when(turnoRepository.existeSolapamiento(anyLong(), any(), any(), any())).thenReturn(true);

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> turnoService.crearTurno(turnoDtoMock, "cliente@test.com"));

        assertEquals("El horario seleccionado ya no está disponible. Elegí otro horario.", excepcion.getMessage());
        assertEquals(HttpStatus.CONFLICT, ((TurnoException) excepcion).getStatus());

        verify(turnoRepository, never()).save(any(Turno.class));
        verify(outboxService, never()).encolar(anyString(), anyString(), any(Object.class));
    }

    @Test
    void crearTurno_FallaPorFechaEnElPasado() {
        turnoDtoMock.setFechaHoraInicio(LocalDateTime.now().minusDays(1));

        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> turnoService.crearTurno(turnoDtoMock, "cliente@test.com"));

        assertEquals("La fecha del turno debe ser a futuro.", excepcion.getMessage());
        verify(usuarioRepository, never()).findByEmail(anyString());
    }
    @ParameterizedTest
    @ValueSource(strings = {"FECHA_NULA", "SERVICIO_NULO", "SERVICIO_CERO", "RECURSO_NEGATIVO"})
    void datosInvalidosNoConsultanInfraestructura(String caso) {
        switch (caso) {
            case "FECHA_NULA" -> turnoDtoMock.setFechaHoraInicio(null);
            case "SERVICIO_NULO" -> turnoDtoMock.setIdServicio(null);
            case "SERVICIO_CERO" -> turnoDtoMock.setIdServicio(0L);
            case "RECURSO_NEGATIVO" -> turnoDtoMock.setIdRecurso(-1L);
        }
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(TurnoException.class,
                () -> turnoService.crearTurno(turnoDtoMock, "cliente@test.com")).getStatus());
        verifyNoInteractions(usuarioRepository, recursoRepository, servicioRepository, turnoRepository, outboxService);
    }

    @Test
    void servicioSinSesionNoCreaTurno() {
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(TurnoException.class,
                () -> turnoService.crearTurno(turnoDtoMock, null)).getStatus());
        verifyNoInteractions(turnoRepository);
    }

    @Test
    void clienteInhabilitadoNoReserva() {
        clienteMock.setEstadoCuenta(EstadoCuenta.SUSPENDIDO);
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(clienteMock));
        assertThrows(AccessDeniedException.class, () -> turnoService.crearTurno(turnoDtoMock, "cliente@test.com"));
        verifyNoInteractions(servicioRepository, recursoRepository, turnoRepository, outboxService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INEXISTENTE", "INACTIVO"})
    void recursoNoDisponibleSeRechaza(String caso) {
        prepararReserva();
        if (caso.equals("INEXISTENTE")) when(recursoRepository.findByIdActivoParaActualizar(1L)).thenReturn(Optional.empty());
        else recursoMock.setActivo(false);
        assertReservaRechazada(HttpStatus.NOT_FOUND);
    }

    @Test
    void negocioInactivoSeRechaza() {
        prepararReserva();
        negocioMock.setActivo(false);
        assertReservaRechazada(HttpStatus.CONFLICT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INEXISTENTE", "INACTIVO"})
    void servicioNoDisponibleSeRechaza(String caso) {
        prepararReserva();
        if (caso.equals("INEXISTENTE")) when(servicioRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.empty());
        else servicioMock.setActivo(false);
        assertReservaRechazada(HttpStatus.NOT_FOUND);
    }

    @Test
    void servicioDeOtroRecursoSeRechaza() {
        prepararReserva();
        turnoDtoMock.setIdRecurso(2L);
        Recurso otro = new Recurso();
        otro.setId(2L);
        when(recursoRepository.findByIdActivoParaActualizar(2L)).thenReturn(Optional.of(otro));
        assertReservaRechazada(HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -30})
    void duracionNoPositivaSeRechaza(int minutos) {
        prepararReserva();
        servicioMock.setDuracionMinutos(minutos);
        assertReservaRechazada(HttpStatus.BAD_REQUEST);
    }

    @Test
    void duracionNulaSeRechaza() {
        prepararReserva();
        servicioMock.setDuracionMinutos(null);
        assertReservaRechazada(HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"08:59", "17:45", "18:00"})
    void intervaloCompletoDebeCaberEnLaJornada(String hora) {
        prepararReserva();
        turnoDtoMock.setFechaHoraInicio(turnoDtoMock.getFechaHoraInicio().with(LocalTime.parse(hora)));
        assertReservaRechazada(HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECURSO", "NEGOCIO"})
    void descansoNoPuedeOcuparseNiAtravesarse(String donde) {
        prepararReserva();
        for (HorarioRecurso h : recursoMock.getHorarios()) {
            if (donde.equals("RECURSO")) { h.setHoraInicioDescanso(LocalTime.NOON); h.setHoraFinDescanso(LocalTime.of(13, 0)); }
        }
        for (HorarioNegocio h : negocioMock.getHorarios()) {
            if (donde.equals("NEGOCIO")) { h.setHoraInicioDescanso(LocalTime.NOON); h.setHoraFinDescanso(LocalTime.of(13, 0)); }
        }
        turnoDtoMock.setFechaHoraInicio(turnoDtoMock.getFechaHoraInicio().with(LocalTime.of(11, 45)));
        assertReservaRechazada(HttpStatus.BAD_REQUEST);
        turnoDtoMock.setFechaHoraInicio(turnoDtoMock.getFechaHoraInicio().with(LocalTime.of(12, 15)));
        assertReservaRechazada(HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SIN_HORARIOS", "DIA_INACTIVO", "NEGOCIO_CERRADO", "FIN_NULO", "DESCANSO_INCOMPLETO", "DESCANSO_FUERA"})
    void configuracionDeJornadaInvalidaNoPermiteReservar(String caso) {
        prepararReserva();
        switch (caso) {
            case "SIN_HORARIOS" -> recursoMock.setHorarios(List.of());
            case "DIA_INACTIVO" -> recursoMock.getHorarios().forEach(h -> h.setActivo(false));
            case "NEGOCIO_CERRADO" -> negocioMock.getHorarios().forEach(h -> h.setAbierto(false));
            case "FIN_NULO" -> recursoMock.getHorarios().forEach(h -> h.setHoraFin(null));
            case "DESCANSO_INCOMPLETO" -> recursoMock.getHorarios().forEach(h -> h.setHoraInicioDescanso(LocalTime.NOON));
            case "DESCANSO_FUERA" -> recursoMock.getHorarios().forEach(h -> {
                h.setHoraInicioDescanso(LocalTime.of(18, 0)); h.setHoraFinDescanso(LocalTime.of(19, 0));
            });
        }
        assertReservaRechazada(HttpStatus.BAD_REQUEST);
    }

    @Test
    void madrugadaSeValidaContraLaJornadaAnteriorSinRecortarElIntervalo() {
        prepararReserva();
        recursoMock.getHorarios().forEach(h -> { h.setHoraInicio(LocalTime.of(22, 0)); h.setHoraFin(LocalTime.of(2, 0)); });
        negocioMock.getHorarios().forEach(h -> { h.setHoraApertura(LocalTime.of(22, 0)); h.setHoraCierre(LocalTime.of(2, 0)); });
        turnoDtoMock.setFechaHoraInicio(turnoDtoMock.getFechaHoraInicio().with(LocalTime.of(0, 30)));
        when(turnoRepository.save(any())).thenAnswer(c -> c.getArgument(0));
        assertEquals(EstadoTurno.CONFIRMADO, turnoService.crearTurno(turnoDtoMock, "cliente@test.com").getEstado());
    }

    @ParameterizedTest
    @ValueSource(strings = {"09:00", "11:30", "13:00", "17:30"})
    void limitesDeJornadaYDescansoPermitenTurnosConsecutivos(String hora) {
        prepararReserva();
        recursoMock.getHorarios().forEach(h -> {
            h.setHoraInicioDescanso(LocalTime.NOON); h.setHoraFinDescanso(LocalTime.of(13, 0));
        });
        negocioMock.getHorarios().forEach(h -> {
            h.setHoraInicioDescanso(LocalTime.NOON); h.setHoraFinDescanso(LocalTime.of(13, 0));
        });
        turnoDtoMock.setFechaHoraInicio(turnoDtoMock.getFechaHoraInicio().with(LocalTime.parse(hora)));
        when(turnoRepository.save(any())).thenAnswer(c -> c.getArgument(0));
        assertEquals(EstadoTurno.CONFIRMADO, turnoService.crearTurno(turnoDtoMock, "cliente@test.com").getEstado());
    }

    @Test
    void disponibilidadNoEntraEnBucleConDuracionCero() {
        servicioMock.setDuracionMinutos(0);
        when(recursoRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(recursoMock));
        when(servicioRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(servicioMock));
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(TurnoException.class,
                () -> turnoService.obtenerHorariosDisponibles(turnoDtoMock.getFechaHoraInicio().toLocalDate(), 1L, 1L)).getStatus());
    }

    @Test
    void cancelacionValidaSeGuardaUnaVezYPublicaEventosExistentes() {
        Turno turno = turnoConfirmado();
        when(turnoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(turno));
        when(turnoRepository.save(any())).thenAnswer(c -> c.getArgument(0));
        assertEquals("Turno cancelado correctamente.", turnoService.cancelarTurno(1L, "cliente@test.com"));
        assertEquals(EstadoTurno.CANCELADO, turno.getEstado());
        assertEquals(HttpStatus.CONFLICT, assertThrows(TurnoException.class,
                () -> turnoService.cancelarTurno(1L, "cliente@test.com")).getStatus());
        verify(turnoRepository, times(1)).save(turno);
        verify(outboxService, times(2)).encolar(anyString(), anyString(), any(Object.class));
    }

    @Test
    void patchCancelarUtilizaLaMismaCancelacion() {
        Turno turno = turnoConfirmado();
        when(turnoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(turno));
        when(turnoRepository.save(any())).thenAnswer(c -> c.getArgument(0));
        assertEquals(EstadoTurno.CANCELADO,
                turnoService.actualizarEstado(1L, EstadoTurno.CANCELADO, "cliente@test.com").getEstado());
        verify(outboxService, times(2)).encolar(anyString(), anyString(), any(Object.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cliente@test.com", "owner@test.com"})
    void nadiePuedeReconfirmarUnTurnoCancelado(String email) {
        Turno turno = turnoConfirmado();
        turno.setEstado(EstadoTurno.CANCELADO);
        when(turnoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(turno));
        assertEquals(HttpStatus.CONFLICT, assertThrows(TurnoException.class,
                () -> turnoService.actualizarEstado(1L, EstadoTurno.CONFIRMADO, email)).getStatus());
        verify(turnoRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void repetirConfirmadoEsIdempotenteYNoVuelveAOcupar() {
        Turno turno = turnoConfirmado();
        when(turnoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(turno));
        assertSame(turno, turnoService.actualizarEstado(1L, EstadoTurno.CONFIRMADO, "cliente@test.com"));
        verify(turnoRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void terceroNoCancelaNiModificaEstado() {
        when(turnoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(turnoConfirmado()));
        assertThrows(AccessDeniedException.class, () -> turnoService.cancelarTurno(1L, "tercero@test.com"));
        assertThrows(AccessDeniedException.class,
                () -> turnoService.actualizarEstado(1L, EstadoTurno.CANCELADO, "tercero@test.com"));
        verify(turnoRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    private void prepararReserva() {
        lenient().when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(clienteMock));
        lenient().when(servicioRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(servicioMock));
        lenient().when(recursoRepository.findByIdActivoParaActualizar(1L)).thenReturn(Optional.of(recursoMock));
    }

    private void assertReservaRechazada(HttpStatus status) {
        assertEquals(status, assertThrows(TurnoException.class,
                () -> turnoService.crearTurno(turnoDtoMock, "cliente@test.com")).getStatus());
        verify(turnoRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    private Turno turnoConfirmado() {
        Turno turno = new Turno();
        turno.setId(1L);
        turno.setCliente(clienteMock);
        turno.setRecurso(recursoMock);
        turno.setServicio(servicioMock);
        turno.setEstado(EstadoTurno.CONFIRMADO);
        turno.setFechaHoraInicio(turnoDtoMock.getFechaHoraInicio());
        turno.setFechaHoraFin(turnoDtoMock.getFechaHoraInicio().plusMinutes(30));
        return turno;
    }
}
