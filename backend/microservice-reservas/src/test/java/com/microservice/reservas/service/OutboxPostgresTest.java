package com.microservice.reservas.service;

import com.microservice.reservas.config.RabbitMQConfig;
import com.microservice.reservas.dto.event.AvisoInternoEvent;
import com.microservice.reservas.dto.request.*;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.enums.EstadoAsociacion;
import com.microservice.reservas.enums.EstadoCuenta;
import com.microservice.reservas.enums.EstadoTurno;
import com.microservice.reservas.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=update", "spring.datasource.hikari.transaction-isolation=TRANSACTION_READ_COMMITTED",
        "spring.datasource.hikari.connection-init-sql=SET statement_timeout = '15s'",
        "app.outbox.confirm-timeout-ms=100", "app.outbox.retry-initial-ms=60000", "app.outbox.retry-max-ms=300000",
        "logging.level.root=WARN", "debug=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OutboxService.class, OutboxPublisher.class, TurnoService.class, RecursoService.class,
        NegocioService.class, RabbitMQConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnabledIfEnvironmentVariable(named = "AGENDAPP_TEST_OUTBOX_POSTGRES_URL", matches = "jdbc:postgresql:.*")
class OutboxPostgresTest {
    @Autowired private TurnoService turnosService;
    @Autowired private RecursoService recursosService;
    @Autowired private OutboxService outboxService;
    @Autowired private OutboxPublisher publisher;
    @Autowired private EventoOutboxRepository outbox;
    @Autowired private TurnoRepository turnos;
    @Autowired private RecursoRepository recursos;
    @Autowired private NegocioRepository negocios;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ServicioRepository servicios;
    @Autowired private PlatformTransactionManager txManager;
    @Autowired private DataSource dataSource;
    @MockitoBean private RabbitTemplate rabbit;

    private TransactionTemplate tx;
    private Long recursoId;
    private Long servicioId;
    private Long negocioId;
    private String cliente;
    private String empleado;
    private LocalDateTime inicio;
    private Set<UUID> anteriores;

    @DynamicPropertySource
    static void baseAislada(DynamicPropertyRegistry registry) {
        String url = System.getenv("AGENDAPP_TEST_OUTBOX_POSTGRES_URL");
        if (url == null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/agendapp_outbox_test")) {
            throw new IllegalArgumentException("Se requiere PostgreSQL local dedicado: agendapp_outbox_test.");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    @BeforeEach
    void preparar() {
        tx = new TransactionTemplate(txManager);
        anteriores = new HashSet<>(outbox.findAll().stream().map(EventoOutbox::getId).toList());
        inicio = LocalDate.now().plusDays(2).atTime(10, 0);
        tx.executeWithoutResult(status -> {
            Usuario owner = usuario("cliente-" + UUID.randomUUID() + "@example.com");
            cliente = owner.getEmail();
            empleado = usuario("empleado-" + UUID.randomUUID() + "@example.com").getEmail();
            Negocio negocio = new Negocio();
            negocio.setAlias("outbox-" + UUID.randomUUID());
            negocio.setNombre("Negocio de prueba");
            negocio.setOwner(owner);
            negocio.setHorarios(new ArrayList<>());
            Recurso recurso = new Recurso();
            recurso.setNombre("Agenda");
            recurso.setNegocio(negocio);
            recurso.setHorarios(new ArrayList<>());
            for (DayOfWeek dia : DayOfWeek.values()) {
                HorarioNegocio hn = new HorarioNegocio();
                hn.setDiaSemana(dia); hn.setHoraApertura(LocalTime.of(9, 0)); hn.setHoraCierre(LocalTime.of(18, 0));
                hn.setNegocio(negocio); negocio.getHorarios().add(hn);
                HorarioRecurso hr = new HorarioRecurso();
                hr.setDiaSemana(dia); hr.setHoraInicio(LocalTime.of(9, 0)); hr.setHoraFin(LocalTime.of(18, 0));
                hr.setRecurso(recurso); recurso.getHorarios().add(hr);
            }
            negocios.save(negocio);
            negocioId = negocio.getId();
            recursos.save(recurso);
            recursoId = recurso.getId();
            Servicio servicio = new Servicio();
            servicio.setNombre("Servicio"); servicio.setDuracionMinutos(30); servicio.setRecurso(recurso);
            servicios.save(servicio);
            servicioId = servicio.getId();
        });
    }

    @AfterEach
    void aislarPendientesDeEsteCasoSinBorrarFilas() {
        // Solo la DB temporal de tests: evita que un caso posterior tome los pendientes de otro.
        tx.executeWithoutResult(status -> outbox.findAll().stream().filter(e -> !anteriores.contains(e.getId())
                && e.getPublicadoEn() == null).forEach(e -> e.setProximoIntento(Instant.now().plusSeconds(86400))));
    }

    @Test
    void reservaSeConfirmaSinContactarRabbitYSePublicaLuegoDelCommit() {
        Turno turno = reservar(inicio);
        assertEquals(EstadoTurno.CONFIRMADO, turnos.findById(turno.getId()).orElseThrow().getEstado());
        assertEquals(2, nuevos().size());
        assertTrue(nuevos().stream().allMatch(e -> e.getPublicadoEn() == null && e.getIntentos() == 0));
        verifyNoInteractions(rabbit);
        confirmarMock();
        assertTrue(publisher.publicarSiguiente());
        assertEquals(1, nuevos().stream().filter(e -> e.getPublicadoEn() != null).count());
    }

    @ParameterizedTest
    @CsvSource({"1,60", "2,120", "3,240", "4,300", "5,300", "100,300"})
    void backoffPersistidoNoReintentaAntesDeFechaNiDescartaPendientes(int fallo, long segundos) {
        tx.executeWithoutResult(status -> outboxService.encolar("notificaciones.exchange", "avisos.internos.routing.key",
                new AvisoInternoEvent(cliente, "Prueba", "Mensaje")));
        String id = nuevos().get(0).getId().toString();
        tx.executeWithoutResult(status -> outbox.findById(UUID.fromString(id)).orElseThrow().setIntentos(fallo - 1));
        doAnswer(c -> { ((CorrelationData) c.getArgument(3)).getFuture().complete(new CorrelationData.Confirm(false, null)); return null; })
                .when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        Instant antes = Instant.now();
        assertTrue(publisher.publicarSiguiente()); // Evento recién creado, elegible sin esperar un minuto.
        EventoOutbox pendiente = outbox.findById(UUID.fromString(id)).orElseThrow();
        assertNull(pendiente.getPublicadoEn());
        assertEquals(fallo, pendiente.getIntentos());
        assertFalse(pendiente.getProximoIntento().isBefore(antes.plusSeconds(segundos)));
        assertFalse(pendiente.getProximoIntento().isAfter(Instant.now().plusSeconds(segundos)));
        assertFalse(publisher.publicarSiguiente()); // PostgreSQL excluye filas futuras, sin contacto AMQP.
        assertEquals(fallo, outbox.findById(UUID.fromString(id)).orElseThrow().getIntentos());
        verify(rabbit, times(1)).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    @Test
    void falloBrokerNoRevierteReservaYReintentoRecuperaElEvento() {
        Turno turno = reservar(inicio);
        doThrow(new AmqpConnectException(new IOException("sin conexión"))).when(rabbit)
                .send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        publisher.publicarSiguiente();
        EventoOutbox fallido = nuevos().stream().filter(e -> e.getIntentos() == 1).findFirst().orElseThrow();
        assertNull(fallido.getPublicadoEn());
        assertEquals("PUBLICACION_FALLIDA", fallido.getUltimoError());
        assertEquals(EstadoTurno.CONFIRMADO, turnos.findById(turno.getId()).orElseThrow().getEstado());
        confirmarMock();
        habilitarReintentos();
        drenarPublisher();
        assertTrue(nuevos().stream().allMatch(e -> e.getPublicadoEn() != null));
        assertEquals(2, outbox.findById(fallido.getId()).orElseThrow().getIntentos());
    }

    @Test
    void rollbackDeNegocioTambienRevierteOutboxSinPublicar() {
        long turnosAntes = turnos.count();
        long eventosAntes = outbox.count();
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            reservar(inicio);
            throw new IllegalStateException("Forzar rollback de prueba");
        }));
        assertEquals(turnosAntes, turnos.count());
        assertEquals(eventosAntes, outbox.count());
        assertFalse(publisher.publicarSiguiente());
        verifyNoInteractions(rabbit);
    }

    @Test
    void eventoNoEsVisibleAlPublicadorAntesDelCommit() throws Exception {
        var guardado = new CountDownLatch(1);
        var commit = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var operacion = executor.submit(() -> tx.executeWithoutResult(status -> {
                reservar(inicio);
                guardado.countDown();
                try { if (!commit.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Tiempo agotado"); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(); }
            }));
            assertTrue(guardado.await(10, TimeUnit.SECONDS));
            assertFalse(publisher.publicarSiguiente());
            verifyNoInteractions(rabbit);
            commit.countDown();
            operacion.get(10, TimeUnit.SECONDS);
            confirmarMock();
            assertTrue(publisher.publicarSiguiente());
        } finally { commit.countDown(); executor.shutdownNow(); }
    }

    @Test
    void cancelacionConfirmadaConservaEventoRecuperable() {
        Turno turno = reservar(inicio);
        turnosService.cancelarTurno(turno.getId(), cliente);
        assertEquals(EstadoTurno.CANCELADO, turnos.findById(turno.getId()).orElseThrow().getEstado());
        assertTrue(nuevos().stream().anyMatch(e -> e.getRoutingKey().equals("turno.cliente.routing.key")
                && e.getPayload().contains("CANCELACION") && e.getPublicadoEn() == null));
        verifyNoInteractions(rabbit);
        confirmarMock();
        drenarPublisher();
        assertTrue(nuevos().stream().allMatch(e -> e.getPublicadoEn() != null));
    }

    @Test
    void invitacionPendienteSeRecuperaSinCambiarTokenNiVencimiento() {
        Recurso recurso = recursosService.crearRecurso(invitacion(), cliente);
        String token = recurso.getTokenInvitacion();
        LocalDateTime vencimiento = recurso.getFechaExpiracionInvitacion();
        assertEquals(EstadoAsociacion.PENDIENTE, recurso.getEstadoAsociacion());
        assertEquals(1, nuevos().size());
        publisher.publicarSiguiente(); // Sin confirmación simulada: timeout.
        assertEquals("TIMEOUT_CONFIRMACION", nuevos().get(0).getUltimoError());
        confirmarMock();
        habilitarReintentos();
        drenarPublisher();
        tx.executeWithoutResult(status -> {
            Recurso persistido = recursos.findById(recurso.getId()).orElseThrow();
            assertEquals(token, persistido.getTokenInvitacion());
            assertTrue(Duration.between(vencimiento, persistido.getFechaExpiracionInvitacion()).abs().compareTo(Duration.ofNanos(1000)) < 0);
            assertEquals(EstadoAsociacion.PENDIENTE, persistido.getEstadoAsociacion());
        });
        assertNotNull(nuevos().get(0).getPublicadoEn());
    }

    @Test
    void dosPublicadoresNoEnvianLaMismaFilaConcurrentemente() throws Exception {
        tx.executeWithoutResult(status -> outboxService.encolar("notificaciones.exchange", "avisos.internos.routing.key",
                new AvisoInternoEvent(cliente, "Prueba", "Mensaje")));
        var enviando = new CountDownLatch(1);
        var confirmar = new CountDownLatch(1);
        doAnswer(c -> {
            enviando.countDown();
            assertTrue(confirmar.await(10, TimeUnit.SECONDS));
            ((CorrelationData) c.getArgument(3)).getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        var executor = Executors.newFixedThreadPool(2);
        try {
            var primero = executor.submit(publisher::publicarSiguiente);
            assertTrue(enviando.await(10, TimeUnit.SECONDS));
            // SKIP LOCKED devuelve sin esperar ni duplicar el envío de la fila tomada.
            assertFalse(executor.submit(publisher::publicarSiguiente).get(2, TimeUnit.SECONDS));
            confirmar.countDown();
            assertTrue(primero.get(10, TimeUnit.SECONDS));
            assertNotNull(nuevos().get(0).getPublicadoEn());
            verify(rabbit, times(1)).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        } finally { confirmar.countDown(); executor.shutdownNow(); }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "AGENDAPP_TEST_RABBIT_PORT", matches = "[0-9]+")
    void brokerRealConfirmaDevuelveYRecuperaPublicaciones() throws Exception {
        int puerto = Integer.parseInt(System.getenv("AGENDAPP_TEST_RABBIT_PORT"));
        assertNotEquals(5672, puerto, "Usar un RabbitMQ temporal distinto del de AgendApp.");
        var factory = new CachingConnectionFactory("127.0.0.1", puerto);
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        factory.setPublisherReturns(true);
        factory.setConnectionTimeout(1000);
        var realRabbit = new RabbitTemplate(factory);
        realRabbit.setMandatory(true);
        realRabbit.setMessageConverter(new JacksonJsonMessageConverter());
        var realPublisher = new OutboxPublisher(outbox, realRabbit, 3000, 1000, 4000);
        var admin = new RabbitAdmin(realRabbit);
        String cola = "outbox-test-" + UUID.randomUUID();
        try {
            admin.declareExchange(new DirectExchange("notificaciones.exchange", true, false));
            admin.declareQueue(new org.springframework.amqp.core.Queue(cola, true));
            for (String ruta : List.of("turno.cliente.routing.key", "avisos.internos.routing.key", "invitacion.email.routing.key")) {
                admin.declareBinding(new Binding(cola, Binding.DestinationType.QUEUE, "notificaciones.exchange", ruta, null));
            }
            Turno turno = reservar(inicio);
            assertNull(realRabbit.receive(cola)); // Ningún envío dentro de la transacción de negocio.
            drenarReal(realPublisher);
            assertTrue(nuevos().stream().allMatch(e -> e.getPublicadoEn() != null));
            Message confirmacion = recibirRuta(realRabbit, cola, "CONFIRMACION");
            assertNotNull(confirmacion.getMessageProperties().getMessageId());
            assertEquals(confirmacion.getMessageProperties().getMessageId(), confirmacion.getMessageProperties().getHeader("eventId"));
            assertEquals(MessageDeliveryMode.PERSISTENT, confirmacion.getMessageProperties().getReceivedDeliveryMode());

            // Conexión realmente rechazada, sin mock; luego se restablece el endpoint del broker temporal.
            int cerrado;
            try (var socket = new ServerSocket(0)) { cerrado = socket.getLocalPort(); }
            factory.resetConnection(); factory.setPort(cerrado);
            turnosService.cancelarTurno(turno.getId(), cliente);
            Recurso invitado = recursosService.crearRecurso(invitacion(), cliente);
            String token = invitado.getTokenInvitacion();
            assertTrue(Boolean.TRUE.equals(tx.execute(status -> realPublisher.publicarSiguiente())));
            assertTrue(nuevos().stream().anyMatch(e -> "PUBLICACION_FALLIDA".equals(e.getUltimoError())));
            assertEquals(EstadoTurno.CANCELADO, turnos.findById(turno.getId()).orElseThrow().getEstado());
            assertEquals(EstadoAsociacion.PENDIENTE, recursos.findById(invitado.getId()).orElseThrow().getEstadoAsociacion());
            factory.resetConnection(); factory.setPort(puerto);
            habilitarReintentos(); drenarReal(realPublisher);
            recibirRuta(realRabbit, cola, "CANCELACION");
            Message invitacion = recibirRuta(realRabbit, cola, "token");
            assertTrue(new String(invitacion.getBody(), StandardCharsets.UTF_8).contains(token));
            assertEquals(token, recursos.findById(invitado.getId()).orElseThrow().getTokenInvitacion());

            String rutaSinDestino = "sin.ruta.test." + UUID.randomUUID();
            tx.executeWithoutResult(status -> outboxService.encolar("notificaciones.exchange", rutaSinDestino,
                    new AvisoInternoEvent(cliente, "Prueba", "Mensaje")));
            assertTrue(Boolean.TRUE.equals(tx.execute(status -> realPublisher.publicarSiguiente())));
            EventoOutbox sinRuta = nuevos().stream().filter(e -> e.getRoutingKey().equals(rutaSinDestino)).findFirst().orElseThrow();
            assertNull(sinRuta.getPublicadoEn());
            assertEquals("SIN_RUTA", sinRuta.getUltimoError());
            admin.declareBinding(new Binding(cola, Binding.DestinationType.QUEUE, "notificaciones.exchange", rutaSinDestino, null));
            habilitarReintentos(); drenarReal(realPublisher);
            assertNotNull(outbox.findById(sinRuta.getId()).orElseThrow().getPublicadoEn());
        } finally {
            factory.setPort(puerto);
            factory.destroy();
        }
    }

    private Message recibirRuta(RabbitTemplate real, String cola, String texto) {
        for (int i = 0; i < 10; i++) {
            Message mensaje = real.receive(cola, 2000);
            if (mensaje == null) break;
            if (new String(mensaje.getBody(), StandardCharsets.UTF_8).contains(texto)) return mensaje;
        }
        throw new AssertionError("No se recibió el tipo de evento esperado del broker temporal.");
    }

    private void drenarReal(OutboxPublisher real) {
        for (int i = 0; i < 20; i++) if (!Boolean.TRUE.equals(tx.execute(status -> real.publicarSiguiente()))) return;
        fail("El lote de prueba debería agotarse.");
    }

    private void confirmarMock() {
        doAnswer(c -> { ((CorrelationData) c.getArgument(3)).getFuture().complete(new CorrelationData.Confirm(true, null)); return null; })
                .when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    private void drenarPublisher() {
        for (int i = 0; i < 20; i++) if (!publisher.publicarSiguiente()) return;
        fail("El lote de prueba debería agotarse.");
    }

    private void habilitarReintentos() {
        tx.executeWithoutResult(status -> outbox.findAll().stream().filter(e -> !anteriores.contains(e.getId())
                && e.getPublicadoEn() == null).forEach(e -> e.setProximoIntento(Instant.now().minusSeconds(1))));
    }

    private List<EventoOutbox> nuevos() {
        return outbox.findAll().stream().filter(e -> !anteriores.contains(e.getId())).toList();
    }

    private Turno reservar(LocalDateTime fecha) {
        TurnoDTO dto = new TurnoDTO(); dto.setIdRecurso(recursoId); dto.setIdServicio(servicioId); dto.setFechaHoraInicio(fecha);
        return turnosService.crearTurno(dto, cliente);
    }

    private Usuario usuario(String email) {
        Usuario u = new Usuario(); u.setEmail(email); u.setPassword(UUID.randomUUID().toString()); u.setEstadoCuenta(EstadoCuenta.ACTIVO);
        return usuarios.save(u);
    }

    private RecursoDTO invitacion() {
        RecursoDTO dto = new RecursoDTO(); dto.setIdNegocio(negocioId); dto.setNombre("Recurso invitado"); dto.setEmailUsuario(empleado);
        dto.setHorarios(Arrays.stream(DayOfWeek.values()).map(dia -> {
            HorarioSemanalDTO h = new HorarioSemanalDTO(); h.setDiaSemana(dia); h.setActivo(true);
            h.setHoraInicio(LocalTime.of(9, 0)); h.setHoraFin(LocalTime.of(18, 0)); return h;
        }).toList());
        return dto;
    }
}
