package com.microservice.reservas.service;

import com.microservice.reservas.dto.request.TurnoDTO;
import com.microservice.reservas.dto.response.HorarioDisponibleDTO;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.microservice.reservas.service.OutboxService;
import org.springframework.http.HttpStatus;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Prueba reglas y paridad GET/POST. La concurrencia se comprueba aparte con PostgreSQL. */
@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {
    @Mock private TurnoRepository turnos;
    @Mock private UsuarioRepository usuarios;
    @Mock private RecursoRepository recursos;
    @Mock private ServicioRepository servicios;
    @Mock private NegocioRepository negocios;
    @Mock private OutboxService outbox;
    @InjectMocks private TurnoService service;

    private final LocalDate fecha = LocalDate.now().plusWeeks(2).with(DayOfWeek.MONDAY);
    private Recurso recurso;
    private Servicio servicio;
    private Negocio negocio;
    private final List<Turno> ocupados = new ArrayList<>();

    @BeforeEach
    void preparar() {
        Usuario cliente = new Usuario();
        cliente.setEmail("cliente@example.com");
        cliente.setEstadoCuenta(EstadoCuenta.ACTIVO);
        negocio = new Negocio();
        negocio.setOwner(cliente);
        negocio.setHorarios(new ArrayList<>());
        recurso = new Recurso();
        recurso.setId(1L);
        recurso.setNegocio(negocio);
        recurso.setHorarios(new ArrayList<>());
        servicio = new Servicio();
        servicio.setId(1L);
        servicio.setRecurso(recurso);
        servicio.setDuracionMinutos(30);
        for (DayOfWeek dia : DayOfWeek.values()) {
            HorarioRecurso hr = new HorarioRecurso();
            hr.setDiaSemana(dia);
            hr.setActivo(false);
            recurso.getHorarios().add(hr);
            HorarioNegocio hn = new HorarioNegocio();
            hn.setDiaSemana(dia);
            hn.setAbierto(false);
            negocio.getHorarios().add(hn);
        }
        lenient().when(recursos.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(recurso));
        lenient().when(recursos.findByIdActivoParaActualizar(1L)).thenReturn(Optional.of(recurso));
        lenient().when(servicios.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(servicio));
        lenient().when(usuarios.findByEmail(cliente.getEmail())).thenReturn(Optional.of(cliente));
        lenient().when(turnos.findTurnosOcupados(eq(1L), any(), any(), eq(EstadoTurno.CONFIRMADO)))
                .thenAnswer(c -> ocupados);
        lenient().when(turnos.existeSolapamiento(eq(1L), any(), any(), eq(EstadoTurno.CONFIRMADO)))
                .thenAnswer(c -> ocupados.stream().anyMatch(t -> t.getFechaHoraInicio().isBefore(c.getArgument(2))
                        && t.getFechaHoraFin().isAfter(c.getArgument(1))));
        lenient().when(turnos.save(any())).thenAnswer(c -> c.getArgument(0));
    }

    @Test
    void jornadaDiurnaOfreceSoloIntervalosCompletos() {
        configurar(fecha, "09:00", "18:00", "10:00", "12:00");
        assertEquals(List.of("10:00", "10:30", "11:00", "11:30"), horas(fecha));
        verificarParidad(fecha);
    }

    @Test
    void interseccionDelNegocioYRecursoSeUsaEnConsultaYCreacion() {
        configurar(fecha, "09:00", "12:00", "10:00", "14:00");
        assertEquals(List.of("10:00", "10:30", "11:00", "11:30"), horas(fecha));
        assertInvalido(fecha.atTime(12, 0));
        verificarParidad(fecha);
    }

    @Test
    void jornadaNocturnaNoRecortaTurnosQueTerminanAMedianoche() {
        configurar(fecha, "22:00", "02:00", "22:30", "01:30");
        assertEquals(List.of("22:30", "23:00", "23:30"), horas(fecha));
        assertEquals(fecha.plusDays(1).atStartOfDay(), reservar(fecha.atTime(23, 30)).getFechaHoraFin());
        verificarParidad(fecha);
    }

    @Test
    void turno2345A0015EsUnUnicoIntervaloYSeOfreceConSuFechaReal() {
        configurar(fecha, "22:00", "02:00", "22:30", "01:30");
        ocupar(fecha.atTime(22, 30), fecha.atTime(23, 45));
        assertEquals(List.of("23:45"), horas(fecha));
        Turno confirmado = reservar(fecha.atTime(23, 45));
        assertEquals(fecha.plusDays(1).atTime(0, 15), confirmado.getFechaHoraFin());
        verify(turnos, atLeastOnce()).findTurnosOcupados(eq(1L), eq(fecha.atTime(22, 30)),
                eq(fecha.plusDays(1).atTime(1, 30)), eq(EstadoTurno.CONFIRMADO));
        verificarParidad(fecha);
    }

    @Test
    void madrugadaDeJornadaAnteriorSigueDisponibleAunqueHoyEsteCerrado() {
        configurar(fecha.minusDays(1), "22:00", "02:00", "22:30", "01:30");
        assertEquals(List.of("00:00", "00:30", "01:00"), horas(fecha));
        verificarParidad(fecha);
        assertInvalido(fecha.atTime(1, 15));
    }

    @Test
    void iniciosDelDiaAnteriorNoSeOfrecenDeNuevoYPermanecenOrdenados() {
        configurar(fecha.minusDays(1), "22:00", "02:00", "22:00", "02:00");
        configurar(fecha, "22:00", "02:00", "22:00", "02:00");
        var lista = disponibles(fecha);
        assertTrue(lista.stream().allMatch(h -> LocalDateTime.parse(h.getFechaHoraISO()).toLocalDate().equals(fecha)));
        assertEquals(lista.size(), lista.stream().map(HorarioDisponibleDTO::getFechaHoraISO).distinct().count());
        assertEquals(lista.stream().map(HorarioDisponibleDTO::getFechaHoraISO).sorted().toList(),
                lista.stream().map(HorarioDisponibleDTO::getFechaHoraISO).toList());
        verificarParidad(fecha);
    }

    @Test
    void descansosDeNegocioYRecursoSeRestanAmbos() {
        configurar(fecha, "09:00", "18:00", "09:00", "18:00");
        hr(fecha).setHoraInicioDescanso(LocalTime.of(11, 0));
        hr(fecha).setHoraFinDescanso(LocalTime.NOON);
        hn(fecha).setHoraInicioDescanso(LocalTime.NOON);
        hn(fecha).setHoraFinDescanso(LocalTime.of(13, 0));
        assertTrue(horas(fecha).containsAll(List.of("10:30", "13:00")));
        assertFalse(horas(fecha).contains("11:00"));
        assertFalse(horas(fecha).contains("12:00"));
        assertInvalido(fecha.atTime(10, 45));
        assertInvalido(fecha.atTime(11, 15));
        assertInvalido(fecha.atTime(12, 15));
        assertDoesNotThrow(() -> reservar(fecha.atTime(10, 30)));
        assertDoesNotThrow(() -> reservar(fecha.atTime(13, 0)));
        verificarParidad(fecha);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NEGOCIO", "RECURSO"})
    void descansoQueCruzaMedianocheRespetaAmbosLimites(String donde) {
        configurar(fecha, "22:00", "02:00", "22:00", "02:00");
        if (donde.equals("NEGOCIO")) {
            hn(fecha).setHoraInicioDescanso(LocalTime.of(23, 45));
            hn(fecha).setHoraFinDescanso(LocalTime.of(0, 15));
        } else {
            hr(fecha).setHoraInicioDescanso(LocalTime.of(23, 45));
            hr(fecha).setHoraFinDescanso(LocalTime.of(0, 15));
        }
        assertFalse(horas(fecha).contains("23:30"));
        assertTrue(horas(fecha.plusDays(1)).contains("00:15"));
        assertInvalido(fecha.atTime(23, 30));
        assertInvalido(fecha.plusDays(1).atStartOfDay());
        assertDoesNotThrow(() -> reservar(fecha.atTime(23, 15)));
        assertDoesNotThrow(() -> reservar(fecha.plusDays(1).atTime(0, 15)));
        verificarParidad(fecha);
        verificarParidad(fecha.plusDays(1));
    }

    @ParameterizedTest
    @ValueSource(ints = {30, 31})
    void duracionDebeCaberHastaElFinalExacto(int minutos) {
        configurar(fecha, "09:00", "18:00", "10:00", "10:30");
        servicio.setDuracionMinutos(minutos);
        if (minutos == 30) {
            assertEquals(List.of("10:00"), horas(fecha));
            verificarParidad(fecha);
        } else {
            assertTrue(disponibles(fecha).isEmpty());
            assertInvalido(fecha.atTime(10, 0));
        }
    }

    @Test
    void ocupacionQueComenzoAyerSeTieneEnCuentaEnLaMadrugada() {
        configurar(fecha.minusDays(1), "22:00", "02:00", "22:00", "02:00");
        ocupar(fecha.minusDays(1).atTime(23, 45), fecha.atTime(0, 15));
        assertFalse(horas(fecha).contains("00:00"));
        assertEquals("00:15", horas(fecha).get(0));
        verificarParidad(fecha);
    }

    @Test
    void ocupacionPosteriorAMedianocheImpideUnInicioDeLaNocheAnterior() {
        configurar(fecha, "22:00", "02:00", "22:00", "02:00");
        ocupar(fecha.atTime(22, 0), fecha.atTime(23, 45));
        ocupar(fecha.plusDays(1).atStartOfDay(), fecha.plusDays(1).atTime(0, 30));
        assertFalse(horas(fecha).contains("23:45"));
        assertEquals(HttpStatus.CONFLICT, assertThrows(TurnoException.class,
                () -> reservar(fecha.atTime(23, 45))).getStatus());
    }

    @Test
    void turnosConsecutivosSonCompatibles() {
        configurar(fecha, "09:00", "18:00", "09:00", "12:00");
        ocupar(fecha.atTime(10, 0), fecha.atTime(10, 30));
        assertTrue(horas(fecha).containsAll(List.of("09:30", "10:30")));
        assertFalse(horas(fecha).contains("10:00"));
        verificarParidad(fecha);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NEGOCIO_INACTIVO", "RECURSO_INACTIVO", "SERVICIO_INACTIVO", "OTRO_RECURSO", "DURACION_CERO"})
    void disponibilidadYReservaRechazanLaMismaConfiguracion(String caso) {
        configurar(fecha, "09:00", "18:00", "09:00", "18:00");
        switch (caso) {
            case "NEGOCIO_INACTIVO" -> negocio.setActivo(false);
            case "RECURSO_INACTIVO" -> recurso.setActivo(false);
            case "SERVICIO_INACTIVO" -> servicio.setActivo(false);
            case "OTRO_RECURSO" -> { Recurso otro = new Recurso(); otro.setId(2L); servicio.setRecurso(otro); }
            case "DURACION_CERO" -> servicio.setDuracionMinutos(0);
        }
        var consulta = assertThrows(TurnoException.class, () -> disponibles(fecha));
        var creacion = assertThrows(TurnoException.class, () -> reservar(fecha.atTime(10, 0)));
        assertEquals(consulta.getStatus(), creacion.getStatus());
    }

    private List<HorarioDisponibleDTO> disponibles(LocalDate dia) {
        return service.obtenerHorariosDisponibles(dia, 1L, 1L);
    }

    private List<String> horas(LocalDate dia) {
        return disponibles(dia).stream().map(HorarioDisponibleDTO::getHora).toList();
    }

    private Turno reservar(LocalDateTime inicio) {
        TurnoDTO dto = new TurnoDTO();
        dto.setIdServicio(1L);
        dto.setIdRecurso(1L);
        dto.setFechaHoraInicio(inicio);
        return service.crearTurno(dto, "cliente@example.com");
    }

    private void verificarParidad(LocalDate dia) {
        var horarios = disponibles(dia);
        assertFalse(horarios.isEmpty());
        for (var horario : horarios) assertDoesNotThrow(() -> reservar(LocalDateTime.parse(horario.getFechaHoraISO())));
    }

    private void assertInvalido(LocalDateTime inicio) {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(TurnoException.class, () -> reservar(inicio)).getStatus());
    }

    private HorarioRecurso hr(LocalDate dia) {
        return recurso.getHorarios().stream().filter(h -> h.getDiaSemana() == dia.getDayOfWeek()).findFirst().orElseThrow();
    }

    private HorarioNegocio hn(LocalDate dia) {
        return negocio.getHorarios().stream().filter(h -> h.getDiaSemana() == dia.getDayOfWeek()).findFirst().orElseThrow();
    }

    private void configurar(LocalDate dia, String apertura, String cierre, String inicio, String fin) {
        hr(dia).setActivo(true);
        hr(dia).setHoraInicio(LocalTime.parse(inicio));
        hr(dia).setHoraFin(LocalTime.parse(fin));
        hn(dia).setAbierto(true);
        hn(dia).setHoraApertura(LocalTime.parse(apertura));
        hn(dia).setHoraCierre(LocalTime.parse(cierre));
    }

    private void ocupar(LocalDateTime inicio, LocalDateTime fin) {
        Turno t = new Turno();
        t.setEstado(EstadoTurno.CONFIRMADO);
        t.setFechaHoraInicio(inicio);
        t.setFechaHoraFin(fin);
        ocupados.add(t);
    }
}
