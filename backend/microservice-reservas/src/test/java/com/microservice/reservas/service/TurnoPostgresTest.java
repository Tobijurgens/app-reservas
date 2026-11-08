package com.microservice.reservas.service;

import com.microservice.reservas.dto.request.TurnoDTO;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.enums.EstadoCuenta;
import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.exception.TurnoException;
import com.microservice.reservas.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.microservice.reservas.service.OutboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.*;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Repositorios, consultas y bloqueos reales. Solo simula RabbitMQ, sin enviar correos. */
@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.datasource.hikari.transaction-isolation=TRANSACTION_READ_COMMITTED",
        "spring.datasource.hikari.connection-init-sql=SET statement_timeout = '15s'",
        "logging.level.root=WARN", "debug=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TurnoService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnabledIfEnvironmentVariable(named = "AGENDAPP_TEST_TURNOS_POSTGRES_URL", matches = "jdbc:postgresql:.*")
class TurnoPostgresTest {
    @Autowired private TurnoService service;
    @Autowired private TurnoRepository turnos;
    @Autowired private RecursoRepository recursos;
    @Autowired private ServicioRepository servicios;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private NegocioRepository negocios;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private DataSource dataSource;
    @MockitoBean private OutboxService outbox;

    private TransactionTemplate transaccion;
    private Datos datos;

    @DynamicPropertySource
    static void baseAislada(DynamicPropertyRegistry registry) {
        String url = System.getenv("AGENDAPP_TEST_TURNOS_POSTGRES_URL");
        if (url == null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/agendapp_turnos_test")) {
            throw new IllegalArgumentException("Usá únicamente una base local dedicada: agendapp_turnos_test.");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    @BeforeEach
    void prepararDatos() {
        transaccion = new TransactionTemplate(transactionManager);
        datos = transaccion.execute(tx -> {
            Usuario cliente = new Usuario();
            cliente.setEmail("cliente-" + UUID.randomUUID() + "@example.com");
            cliente.setPassword(UUID.randomUUID().toString());
            cliente.setEstadoCuenta(EstadoCuenta.ACTIVO);
            usuarios.save(cliente);
            Negocio negocio = new Negocio();
            negocio.setAlias("prueba-" + UUID.randomUUID());
            negocio.setNombre("Negocio de prueba");
            negocio.setOwner(cliente);
            negocio.setHorarios(new ArrayList<>());
            for (DayOfWeek dia : DayOfWeek.values()) {
                HorarioNegocio h = new HorarioNegocio();
                h.setDiaSemana(dia);
                h.setHoraApertura(LocalTime.of(9, 0));
                h.setHoraCierre(LocalTime.of(18, 0));
                h.setNegocio(negocio);
                negocio.getHorarios().add(h);
            }
            negocios.save(negocio);
            Recurso recurso = new Recurso();
            recurso.setNombre("Recurso de prueba");
            recurso.setNegocio(negocio);
            recurso.setHorarios(new ArrayList<>());
            for (DayOfWeek dia : DayOfWeek.values()) {
                HorarioRecurso h = new HorarioRecurso();
                h.setDiaSemana(dia);
                h.setHoraInicio(LocalTime.of(9, 0));
                h.setHoraFin(LocalTime.of(18, 0));
                h.setRecurso(recurso);
                recurso.getHorarios().add(h);
            }
            recursos.save(recurso);
            Servicio servicio = new Servicio();
            servicio.setNombre("Servicio de prueba");
            servicio.setDuracionMinutos(30);
            servicio.setRecurso(recurso);
            servicios.save(servicio);
            return new Datos(recurso.getId(), servicio.getId(), cliente.getId(), cliente.getEmail(),
                    LocalDate.now().plusDays(2).atTime(10, 0));
        });
    }

    @ParameterizedTest
    @CsvSource({"-30,0,false", "30,60,false", "0,30,true", "-15,15,true", "15,45,true",
            "-15,45,true", "5,15,true", "-60,0,false", "60,90,false"})
    void consultasDetectanTodosLosSolapamientosConIntervalosSemiabiertos(int inicio, int fin, boolean ocupado) {
        guardarTurno(EstadoTurno.CONFIRMADO);
        transaccion.executeWithoutResult(tx -> {
            LocalDateTime desde = datos.inicio().plusMinutes(inicio);
            LocalDateTime hasta = datos.inicio().plusMinutes(fin);
            assertEquals(ocupado, turnos.existeSolapamiento(datos.recursoId(), desde, hasta, EstadoTurno.CONFIRMADO));
            assertEquals(ocupado ? 1 : 0,
                    turnos.findTurnosOcupados(datos.recursoId(), desde, hasta, EstadoTurno.CONFIRMADO).size());
        });
    }

    @Test
    void canceladosNoOcupanYLiberanElIntervaloParaUnaReservaNueva() {
        guardarTurno(EstadoTurno.CANCELADO);
        assertEquals(HttpStatus.OK, reservar(0));
        assertEquals(1, confirmados());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -15, 15})
    void crearRechazaSuperposicionPersistida(int desfase) {
        guardarTurno(EstadoTurno.CONFIRMADO);
        assertEquals(HttpStatus.CONFLICT, reservar(desfase));
        assertEquals(1, confirmados());
        verifyNoInteractions(outbox);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 15, -15, 30, -30})
    void reservasConcurrentesSoloConfirmanIntervalosCompatibles(int desfaseSegunda) throws Exception {
        var primeraDentroTransaccion = new CountDownLatch(1);
        var permitirCommit = new CountDownLatch(1);
        doAnswer(invocation -> {
            primeraDentroTransaccion.countDown();
            if (!permitirCommit.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Tiempo de prueba agotado.");
            return null;
        }).when(outbox).encolar(eq("notificaciones.exchange"), eq("turno.cliente.routing.key"), any(Object.class));

        var executor = Executors.newFixedThreadPool(2);
        try {
            var primera = executor.submit(() -> reservar(0));
            assertTrue(primeraDentroTransaccion.await(10, TimeUnit.SECONDS));
            var segunda = executor.submit(() -> reservar(desfaseSegunda));
            assertEsperaBloqueoReal();
            assertFalse(segunda.isDone());
            permitirCommit.countDown();
            assertEquals(HttpStatus.OK, primera.get(10, TimeUnit.SECONDS));
            boolean consecutivas = Math.abs(desfaseSegunda) == 30;
            assertEquals(consecutivas ? HttpStatus.OK : HttpStatus.CONFLICT, segunda.get(10, TimeUnit.SECONDS));
            assertEquals(consecutivas ? 2 : 1, confirmados());
            // Se comprueban las filas persistidas, no solo los resultados de las llamadas.
            transaccion.executeWithoutResult(tx -> {
                var persistidos = turnos.findTurnosOcupados(datos.recursoId(), datos.inicio().minusHours(1),
                        datos.inicio().plusHours(2), EstadoTurno.CONFIRMADO);
                for (int i = 1; i < persistidos.size(); i++) {
                    assertFalse(persistidos.get(i).getFechaHoraInicio().isBefore(persistidos.get(i - 1).getFechaHoraFin()));
                }
            });
            verify(outbox, times(consecutivas ? 2 : 1)).encolar(
                    eq("notificaciones.exchange"), eq("turno.cliente.routing.key"), any(Object.class));
        } finally {
            permitirCommit.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void cancelacionesConcurrentesSoloAplicanUnaCancelacion() throws Exception {
        Long id = guardarTurno(EstadoTurno.CONFIRMADO);
        var primeraDentroTransaccion = new CountDownLatch(1);
        var permitirCommit = new CountDownLatch(1);
        doAnswer(invocation -> {
            primeraDentroTransaccion.countDown();
            if (!permitirCommit.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Tiempo de prueba agotado.");
            return null;
        }).when(outbox).encolar(eq("notificaciones.exchange"), eq("turno.cliente.routing.key"), any(Object.class));
        var executor = Executors.newFixedThreadPool(2);
        try {
            var primera = executor.submit(() -> cancelar(id));
            assertTrue(primeraDentroTransaccion.await(10, TimeUnit.SECONDS));
            var segunda = executor.submit(() -> cancelar(id));
            assertEsperaBloqueoReal();
            permitirCommit.countDown();
            assertEquals(HttpStatus.OK, primera.get(10, TimeUnit.SECONDS));
            assertEquals(HttpStatus.CONFLICT, segunda.get(10, TimeUnit.SECONDS));
            assertEquals(0, confirmados());
            verify(outbox, times(1)).encolar(eq("notificaciones.exchange"), eq("turno.cliente.routing.key"), any(Object.class));
        } finally {
            permitirCommit.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private void assertEsperaBloqueoReal() throws InterruptedException {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < limite) {
            if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM pg_stat_activity "
                    + "WHERE datname = current_database() AND wait_event_type = 'Lock' "
                    + "AND cardinality(pg_blocking_pids(pid)) > 0)", Boolean.class))) return;
            Thread.sleep(25);
        }
        fail("La segunda solicitud debe esperar un bloqueo real de PostgreSQL.");
    }

    @Test
    void disponibilidadNocturnaYCreacionConservanUnIntervaloQueCruzaMedianoche() {
        configurarNocturna();
        LocalDate dia = datos.inicio().toLocalDate();
        guardarTurno(EstadoTurno.CONFIRMADO, dia.atTime(22, 30), dia.atTime(23, 45));
        var ofrecidos = service.obtenerHorariosDisponibles(dia, datos.recursoId(), datos.servicioId());
        assertEquals(1, ofrecidos.size());
        assertEquals(dia.atTime(23, 45), LocalDateTime.parse(ofrecidos.get(0).getFechaHoraISO()));
        Turno confirmado = reservarEn(dia.atTime(23, 45));
        assertEquals(dia.plusDays(1).atTime(0, 15), confirmado.getFechaHoraFin());
        transaccion.executeWithoutResult(tx -> {
            Turno persistido = turnos.findById(confirmado.getId()).orElseThrow();
            assertEquals(dia.atTime(23, 45), persistido.getFechaHoraInicio());
            assertEquals(dia.plusDays(1).atTime(0, 15), persistido.getFechaHoraFin());
        });
    }

    @Test
    void ocupacionIniciadaAyerBloqueaMadrugadaYPermiteConsecutivo() {
        configurarNocturna();
        LocalDate dia = datos.inicio().toLocalDate();
        guardarTurno(EstadoTurno.CONFIRMADO, dia.atTime(23, 45), dia.plusDays(1).atTime(0, 15));
        var ofrecidos = service.obtenerHorariosDisponibles(dia.plusDays(1), datos.recursoId(), datos.servicioId());
        assertEquals(dia.plusDays(1).atTime(0, 15), LocalDateTime.parse(ofrecidos.get(0).getFechaHoraISO()));
        assertEquals(HttpStatus.CONFLICT, assertThrows(TurnoException.class,
                () -> reservarEn(dia.plusDays(1).atStartOfDay())).getStatus());
        assertEquals(EstadoTurno.CONFIRMADO, reservarEn(dia.plusDays(1).atTime(0, 15)).getEstado());
    }

    @Test
    void descansoNocturnoDeNegocioSeRespetaConDatosPersistidos() {
        configurarNocturna();
        LocalDate dia = datos.inicio().toLocalDate();
        transaccion.executeWithoutResult(tx -> {
            Recurso recurso = recursos.findById(datos.recursoId()).orElseThrow();
            recurso.getNegocio().getHorarios().stream().filter(h -> h.getDiaSemana() == dia.getDayOfWeek()).forEach(h -> {
                h.setHoraInicioDescanso(LocalTime.of(23, 45));
                h.setHoraFinDescanso(LocalTime.of(0, 15));
            });
        });
        var ofrecidos = service.obtenerHorariosDisponibles(dia, datos.recursoId(), datos.servicioId());
        assertTrue(ofrecidos.stream().noneMatch(h -> h.getHora().equals("23:30")));
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(TurnoException.class, () -> reservarEn(dia.atTime(23, 30))).getStatus());
        assertEquals(EstadoTurno.CONFIRMADO, reservarEn(dia.atTime(23, 15)).getEstado());
        assertEquals(EstadoTurno.CONFIRMADO, reservarEn(dia.plusDays(1).atTime(0, 15)).getEstado());
    }

    private void configurarNocturna() {
        transaccion.executeWithoutResult(tx -> {
            Recurso recurso = recursos.findById(datos.recursoId()).orElseThrow();
            recurso.getHorarios().forEach(h -> {
                h.setActivo(h.getDiaSemana() == datos.inicio().getDayOfWeek());
                h.setHoraInicio(LocalTime.of(22, 30));
                h.setHoraFin(LocalTime.of(1, 30));
            });
            recurso.getNegocio().getHorarios().forEach(h -> {
                h.setAbierto(h.getDiaSemana() == datos.inicio().getDayOfWeek());
                h.setHoraApertura(LocalTime.of(22, 0));
                h.setHoraCierre(LocalTime.of(2, 0));
            });
        });
    }

    private Turno reservarEn(LocalDateTime inicio) {
        TurnoDTO dto = new TurnoDTO();
        dto.setIdRecurso(datos.recursoId());
        dto.setIdServicio(datos.servicioId());
        dto.setFechaHoraInicio(inicio);
        return service.crearTurno(dto, datos.email());
    }

    private HttpStatus reservar(int desfase) {
        TurnoDTO dto = new TurnoDTO();
        dto.setIdRecurso(datos.recursoId());
        dto.setIdServicio(datos.servicioId());
        dto.setFechaHoraInicio(datos.inicio().plusMinutes(desfase));
        try {
            service.crearTurno(dto, datos.email());
            return HttpStatus.OK;
        } catch (TurnoException ex) {
            return ex.getStatus();
        }
    }

    private HttpStatus cancelar(Long id) {
        try {
            service.cancelarTurno(id, datos.email());
            return HttpStatus.OK;
        } catch (TurnoException ex) {
            return ex.getStatus();
        }
    }

    private Long guardarTurno(EstadoTurno estado) {
        return guardarTurno(estado, datos.inicio(), datos.inicio().plusMinutes(30));
    }

    private Long guardarTurno(EstadoTurno estado, LocalDateTime inicio, LocalDateTime fin) {
        return transaccion.execute(tx -> {
            Turno turno = new Turno();
            turno.setEstado(estado);
            turno.setFechaHoraInicio(inicio);
            turno.setFechaHoraFin(fin);
            turno.setCliente(usuarios.getReferenceById(datos.clienteId()));
            turno.setRecurso(recursos.getReferenceById(datos.recursoId()));
            turno.setServicio(servicios.getReferenceById(datos.servicioId()));
            return turnos.save(turno).getId();
        });
    }

    private int confirmados() {
        return transaccion.execute(tx -> (int) turnos.findByRecursoId(datos.recursoId()).stream()
                .filter(t -> t.getEstado() == EstadoTurno.CONFIRMADO).count());
    }

    private record Datos(Long recursoId, Long servicioId, Long clienteId, String email, LocalDateTime inicio) {}
}
