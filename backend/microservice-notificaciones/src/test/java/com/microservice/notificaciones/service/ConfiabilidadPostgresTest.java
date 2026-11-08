package com.microservice.notificaciones.service;

import com.microservice.notificaciones.config.RabbitMQConfig;
import com.microservice.notificaciones.dto.UsuarioRegistradoEvent;
import com.microservice.notificaciones.entity.EventoRecibido;
import com.microservice.notificaciones.enums.*;
import com.microservice.notificaciones.listener.NotificacionListener;
import com.microservice.notificaciones.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MethodRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import tools.jackson.databind.json.JsonMapper;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = ConfiabilidadPostgresTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.jpa.hibernate.ddl-auto=update", "spring.jpa.show-sql=false",
                "spring.datasource.hikari.transaction-isolation=TRANSACTION_READ_COMMITTED",
                "spring.datasource.hikari.connection-init-sql=SET statement_timeout = '15s'",
                "spring.rabbitmq.listener.simple.auto-startup=false", "app.notificaciones.enabled=false",
                "spring.mail.host=127.0.0.1", "spring.mail.port=9", "spring.mail.username=test@example.com",
                "spring.mail.password=", "logging.level.root=WARN", "debug=false"})
@EnabledIfEnvironmentVariable(named = "AGENDAPP_TEST_NOTIFICACIONES_POSTGRES_URL", matches = "jdbc:postgresql:.*")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfiabilidadPostgresTest {
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.microservice.notificaciones.entity")
    @EnableJpaRepositories("com.microservice.notificaciones.repository")
    @Import({RecepcionEventoService.class, ProcesadorNotificacion.class})
    static class Config {
        @Bean UserDetailsService usuariosDePrueba() {
            return nombre -> { throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Sin usuarios de test"); };
        }
    }

    @Autowired private RecepcionEventoService recepcion;
    @Autowired private ProcesadorNotificacion procesador;
    @Autowired private EventoRecibidoRepository eventos;
    @Autowired private HistorialNotificacionRepository historiales;
    @Autowired private PlatformTransactionManager txManager;
    @MockitoBean private EmailService email;
    private TransactionTemplate tx;
    private Set<String> anteriores;
    private static final String REINICIO_ID = UUID.randomUUID().toString();

    @DynamicPropertySource
    static void baseAislada(DynamicPropertyRegistry registry) {
        String url = System.getenv("AGENDAPP_TEST_NOTIFICACIONES_POSTGRES_URL");
        if (url == null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/agendapp_notificaciones_test"))
            throw new IllegalArgumentException("Se requiere la base local exclusiva agendapp_notificaciones_test.");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    @BeforeEach
    void preparar() {
        tx = new TransactionTemplate(txManager);
        anteriores = new HashSet<>(eventos.findAll().stream().map(EventoRecibido::getEventId).toList());
    }

    @AfterEach
    void aislarSoloLosPendientesDeEsteCasoSinBorrarFilas() {
        tx.executeWithoutResult(status -> eventos.findAll().stream().filter(e -> !anteriores.contains(e.getEventId())
                && e.getEstado() == EstadoProcesamiento.PENDIENTE).forEach(e -> e.setProximoIntento(Instant.now().plusSeconds(86400))));
    }

    @Test @Order(1)
    void eventoNuevoSeProcesaUnaVezAunqueSeEntregueDosVeces() {
        Message mensaje = registro(REINICIO_ID);
        long antes = historiales.count();
        recepcion.registrar(mensaje, TipoEventoRecibido.REGISTRO);
        recepcion.registrar(mensaje, TipoEventoRecibido.REGISTRO);
        verifyNoInteractions(email);
        assertTrue(procesador.procesarSiguiente());
        assertFalse(procesador.procesarSiguiente());
        recepcion.registrar(mensaje, TipoEventoRecibido.REGISTRO);
        assertFalse(procesador.procesarSiguiente());
        assertEquals(antes + 1, historiales.count());
        assertEquals(EstadoProcesamiento.COMPLETADO, eventos.findById(REINICIO_ID).orElseThrow().getEstado());
        verify(email, times(1)).enviarCorreoVerificacion(anyString(), anyString(), anyString());
    }

    @Test @Order(2)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void duplicadoTrasRecrearContextoNoVuelveAEnviar() {
        assertEquals(EstadoProcesamiento.COMPLETADO, eventos.findById(REINICIO_ID).orElseThrow().getEstado());
        recepcion.registrar(registro(REINICIO_ID), TipoEventoRecibido.REGISTRO);
        assertFalse(procesador.procesarSiguiente());
        verifyNoInteractions(email);
    }

    @Test
    void dosReceptoresConcurrentesCompartenUnaSolaClavePersistente() throws Exception {
        String id = UUID.randomUUID().toString();
        Message mensaje = registro(id);
        var insertado = new CountDownLatch(1); var commit = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var primera = executor.submit(() -> tx.executeWithoutResult(status -> {
                recepcion.registrar(mensaje, TipoEventoRecibido.REGISTRO);
                insertado.countDown(); esperar(commit);
            }));
            assertTrue(insertado.await(5, TimeUnit.SECONDS));
            var segunda = executor.submit(() -> recepcion.registrar(mensaje, TipoEventoRecibido.REGISTRO));
            assertThrows(TimeoutException.class, () -> segunda.get(100, TimeUnit.MILLISECONDS));
            commit.countDown(); primera.get(5, TimeUnit.SECONDS); segunda.get(5, TimeUnit.SECONDS);
            assertEquals(1, eventos.findAll().stream().filter(e -> e.getEventId().equals(id)).count());
            assertTrue(procesador.procesarSiguiente());
            verify(email, times(1)).enviarCorreoVerificacion(anyString(), anyString(), anyString());
        } finally { commit.countDown(); executor.shutdownNow(); }
    }

    @Test
    void dosWorkersNoEnvianElMismoEventoConcurrentemente() throws Exception {
        recepcion.registrar(registro(UUID.randomUUID().toString()), TipoEventoRecibido.REGISTRO);
        var enviando = new CountDownLatch(1); var finalizar = new CountDownLatch(1);
        doAnswer(c -> { enviando.countDown(); esperar(finalizar); return null; })
                .when(email).enviarCorreoVerificacion(anyString(), anyString(), anyString());
        var executor = Executors.newFixedThreadPool(2);
        try {
            var primero = executor.submit(procesador::procesarSiguiente);
            assertTrue(enviando.await(5, TimeUnit.SECONDS));
            assertFalse(executor.submit(procesador::procesarSiguiente).get(1, TimeUnit.SECONDS));
            finalizar.countDown(); assertTrue(primero.get(5, TimeUnit.SECONDS));
            verify(email, times(1)).enviarCorreoVerificacion(anyString(), anyString(), anyString());
        } finally { finalizar.countDown(); executor.shutdownNow(); }
    }

    @Test
    void rollbackDeRecepcionNoImpideUnaEntregaPosterior() {
        String id = UUID.randomUUID().toString();
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            recepcion.registrar(registro(id), TipoEventoRecibido.REGISTRO);
            throw new IllegalStateException("Forzar rollback");
        }));
        assertFalse(eventos.existsById(id));
        recepcion.registrar(registro(id), TipoEventoRecibido.REGISTRO);
        assertTrue(procesador.procesarSiguiente());
        assertEquals(EstadoProcesamiento.COMPLETADO, eventos.findById(id).orElseThrow().getEstado());
    }

    @Test
    void mensajeLegacySinIdNoSeDuplicaInclusoConOtroReceptor() {
        Message m = registro(null);
        String id = IdentidadEvento.obtener(m, TipoEventoRecibido.REGISTRO);
        recepcion.registrar(m, TipoEventoRecibido.REGISTRO);
        assertTrue(procesador.procesarSiguiente());
        tx.executeWithoutResult(status -> new RecepcionEventoService(eventos).registrar(m, TipoEventoRecibido.REGISTRO));
        assertFalse(procesador.procesarSiguiente());
        assertEquals(EstadoProcesamiento.COMPLETADO, eventos.findById(id).orElseThrow().getEstado());
        verify(email, times(1)).enviarCorreoVerificacion(anyString(), anyString(), anyString());
    }

    @Test
    void reintentoFuturoNoContactaSmtpYLimiteConservaFallo() {
        String id = UUID.randomUUID().toString();
        recepcion.registrar(registro(id), TipoEventoRecibido.REGISTRO);
        doThrow(new MailSendException("temporal", new java.net.SocketTimeoutException()))
                .when(email).enviarCorreoVerificacion(anyString(), anyString(), anyString());
        assertTrue(procesador.procesarSiguiente());
        assertFalse(procesador.procesarSiguiente());
        verify(email, times(1)).enviarCorreoVerificacion(anyString(), anyString(), anyString());
        for (int i = 0; i < 4; i++) { habilitar(id); assertTrue(procesador.procesarSiguiente()); }
        EventoRecibido e = eventos.findById(id).orElseThrow();
        assertEquals(EstadoProcesamiento.FALLIDO, e.getEstado());
        assertEquals(5, e.getIntentos());
        assertNotNull(e.getHistorialId());
        assertEquals(EstadoNotificacion.ERROR, historiales.findById(e.getHistorialId()).orElseThrow().getEstado());
        assertFalse(procesador.procesarSiguiente());
        verify(email, times(5)).enviarCorreoVerificacion(anyString(), anyString(), anyString());
    }

    @Test
    void servidorSmtpControladoRecupera451YDetecta550Permanente() throws Exception {
        try (var smtp = new ServidorSmtpPrueba()) {
            var real = procesadorReal(smtp.puerto());
            String id = UUID.randomUUID().toString();
            recepcion.registrar(registro(id), TipoEventoRecibido.REGISTRO);
            smtp.responderDestinatario(451);
            procesarReal(real);
            assertEquals(EstadoProcesamiento.PENDIENTE, eventos.findById(id).orElseThrow().getEstado());
            assertEquals(0, smtp.aceptados());
            smtp.responderDestinatario(250); habilitar(id); procesarReal(real);
            assertEquals(EstadoProcesamiento.COMPLETADO, eventos.findById(id).orElseThrow().getEstado());
            assertEquals(1, smtp.aceptados());
            recepcion.registrar(registro(id), TipoEventoRecibido.REGISTRO);
            assertFalse(procesarReal(real));
            assertEquals(1, smtp.aceptados());
            String rechazado = UUID.randomUUID().toString();
            recepcion.registrar(registro(rechazado), TipoEventoRecibido.REGISTRO);
            smtp.responderDestinatario(550); procesarReal(real);
            EventoRecibido fallo = eventos.findById(rechazado).orElseThrow();
            assertEquals(EstadoProcesamiento.FALLIDO, fallo.getEstado());
            assertEquals(1, fallo.getIntentos());
            assertNull(fallo.getProximoIntento());
            assertFalse(procesarReal(real));
        }
    }

    @Test
    void timeoutSmtpRealSeRecuperaYNoDejaEstadoProcesando() throws Exception {
        try (var smtp = new ServidorSmtpPrueba()) {
            String id = UUID.randomUUID().toString();
            recepcion.registrar(registro(id), TipoEventoRecibido.REGISTRO);
            smtp.simularTimeout(true); var real = procesadorReal(smtp.puerto()); procesarReal(real);
            assertEquals(EstadoProcesamiento.PENDIENTE, eventos.findById(id).orElseThrow().getEstado());
            assertEquals("SMTP_TIMEOUT", eventos.findById(id).orElseThrow().getUltimoError());
            smtp.simularTimeout(false); habilitar(id); procesarReal(real);
            assertEquals(EstadoProcesamiento.COMPLETADO, eventos.findById(id).orElseThrow().getEstado());
            assertEquals(1, smtp.aceptados());
        }
    }

    @Test
    void smtpDesconectadoRealSeRecuperaEnElSiguienteIntento() throws Exception {
        int puerto;
        try (var socket = new java.net.ServerSocket(0, 10, java.net.InetAddress.getByName("127.0.0.1"))) { puerto = socket.getLocalPort(); }
        String id = UUID.randomUUID().toString();
        recepcion.registrar(registro(id), TipoEventoRecibido.REGISTRO);
        var real = procesadorReal(puerto); procesarReal(real);
        assertEquals(EstadoProcesamiento.PENDIENTE, eventos.findById(id).orElseThrow().getEstado());
        assertEquals("SMTP_CONEXION", eventos.findById(id).orElseThrow().getUltimoError());
        try (var smtp = new ServidorSmtpPrueba(puerto)) {
            habilitar(id); procesarReal(real);
            assertEquals(EstadoProcesamiento.COMPLETADO, eventos.findById(id).orElseThrow().getEstado());
            assertEquals(2, eventos.findById(id).orElseThrow().getIntentos());
            assertEquals(1, smtp.aceptados());
        }
    }

    @Test
    void plantillaRealInexistenteConservaFalloDefinitivoSinContactarSmtp() throws Exception {
        try (var smtp = new ServidorSmtpPrueba()) {
            String id = UUID.randomUUID().toString();
            recepcion.registrar(registro(id), TipoEventoRecibido.REGISTRO);
            procesarReal(procesadorReal(smtp.puerto(), true));
            EventoRecibido e = eventos.findById(id).orElseThrow();
            assertEquals(EstadoProcesamiento.FALLIDO, e.getEstado());
            assertEquals("PLANTILLA_INVALIDA", e.getUltimoError());
            assertEquals(0, smtp.aceptados());
        }
    }

    @Test
    void rollbackTrasAceptacionSmtpReconoceLaVentanaDeDuplicado() throws Exception {
        try (var smtp = new ServidorSmtpPrueba()) {
            String id = UUID.randomUUID().toString();
            recepcion.registrar(registro(id), TipoEventoRecibido.REGISTRO);
            var real = procesadorReal(smtp.puerto());
            tx.executeWithoutResult(status -> { real.procesarSiguiente(); status.setRollbackOnly(); });
            EventoRecibido pendiente = eventos.findById(id).orElseThrow();
            assertEquals(EstadoProcesamiento.PENDIENTE, pendiente.getEstado());
            assertNull(pendiente.getHistorialId());
            assertEquals(1, smtp.aceptados());
            procesarReal(real);
            assertEquals(EstadoProcesamiento.COMPLETADO, eventos.findById(id).orElseThrow().getEstado());
            assertEquals(2, smtp.aceptados()); // SMTP y PostgreSQL no comparten una transacción atómica.
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "AGENDAPP_TEST_RABBIT_PORT", matches = "[0-9]+")
    void rabbitRealNackRedeliveryAckDeduplicacionYMensajeMalformado() throws Exception {
        int puerto = Integer.parseInt(System.getenv("AGENDAPP_TEST_RABBIT_PORT"));
        assertNotEquals(5672, puerto);
        var cf = new CachingConnectionFactory("127.0.0.1", puerto);
        cf.setConnectionTimeout(1000);
        var rabbit = new RabbitTemplate(cf); var admin = new RabbitAdmin(rabbit);
        String cola = "notificaciones-test-" + UUID.randomUUID();
        admin.declareExchange(new DirectExchange(RabbitMQConfig.EXCHANGE_NOTIFICACIONES));
        admin.declareQueue(new org.springframework.amqp.core.Queue(cola, true));
        admin.declareBinding(new Binding(cola, Binding.DestinationType.QUEUE, RabbitMQConfig.EXCHANGE_NOTIFICACIONES,
                RabbitMQConfig.ROUTING_KEY_REGISTRO, null));
        AtomicInteger llamadas = new AtomicInteger(); AtomicBoolean redelivery = new AtomicBoolean();
        NotificacionListener listener = new NotificacionListener(recepcion) {
            @Override public void procesarRegistroUsuario(Message mensaje) {
                int numero = llamadas.incrementAndGet();
                if (Boolean.TRUE.equals(mensaje.getMessageProperties().isRedelivered())) redelivery.set(true);
                if (numero <= 4) throw new org.springframework.dao.TransientDataAccessResourceException("Simular DB no disponible");
                super.procesarRegistroUsuario(mensaje);
            }
        };
        SimpleMessageListenerContainer container = contenedor(cf, listener, cola);
        try {
            container.start();
            String id = UUID.randomUUID().toString();
            rabbit.send(RabbitMQConfig.EXCHANGE_NOTIFICACIONES, RabbitMQConfig.ROUTING_KEY_REGISTRO, registro(id));
            hasta(() -> eventos.existsById(id), 10000);
            assertTrue(redelivery.get()); assertTrue(llamadas.get() >= 5);
            verifyNoInteractions(email);
            assertTrue(procesador.procesarSiguiente());
            assertEquals(EstadoProcesamiento.COMPLETADO, eventos.findById(id).orElseThrow().getEstado());
            container.stop();
            AtomicInteger duplicados = new AtomicInteger();
            container = contenedor(cf, new NotificacionListener(recepcion) {
                @Override public void procesarRegistroUsuario(Message mensaje) {
                    super.procesarRegistroUsuario(mensaje); duplicados.incrementAndGet();
                }
            }, cola); container.start();
            rabbit.send(RabbitMQConfig.EXCHANGE_NOTIFICACIONES, RabbitMQConfig.ROUTING_KEY_REGISTRO, registro(id));
            hasta(() -> duplicados.get() >= 1, 5000);
            assertFalse(procesador.procesarSiguiente());
            verify(email, times(1)).enviarCorreoVerificacion(anyString(), anyString(), anyString());
            String malId = UUID.randomUUID().toString();
            MessageProperties props = new MessageProperties(); props.setHeader("eventId", malId);
            props.setContentType("application/x-java-serialized-object");
            rabbit.send(RabbitMQConfig.EXCHANGE_NOTIFICACIONES, RabbitMQConfig.ROUTING_KEY_REGISTRO,
                    new Message(new byte[] {0, 1, 2}, props));
            hasta(() -> eventos.existsById(malId), 5000);
            assertTrue(procesador.procesarSiguiente());
            assertEquals(EstadoProcesamiento.FALLIDO, eventos.findById(malId).orElseThrow().getEstado());
            container.stop();
            assertEquals(0, mensajesListos(admin, cola)); // No queda trabajo no reconocido para otro consumidor.
        } finally { container.stop(); cf.destroy(); }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "AGENDAPP_TEST_RABBIT_PORT", matches = "[0-9]+")
    void perdidaDeAckTrasCommitProduceRedeliverySinDuplicarEnvio() throws Exception {
        int puerto = Integer.parseInt(System.getenv("AGENDAPP_TEST_RABBIT_PORT")); assertNotEquals(5672, puerto);
        var cf = new CachingConnectionFactory("127.0.0.1", puerto);
        var rabbit = new RabbitTemplate(cf); var admin = new RabbitAdmin(rabbit);
        String cola = "ack-test-" + UUID.randomUUID();
        admin.declareQueue(new org.springframework.amqp.core.Queue(cola, true));
        AtomicInteger entregas = new AtomicInteger();
        NotificacionListener listener = new NotificacionListener(recepcion) {
            @Override public void procesarRegistroUsuario(Message mensaje) {
                super.procesarRegistroUsuario(mensaje); // DB ya confirmó; AUTO todavía no envió ACK.
                if (entregas.incrementAndGet() == 1) cf.resetConnection();
            }
        };
        var container = contenedor(cf, listener, cola);
        try {
            container.start(); String id = UUID.randomUUID().toString();
            rabbit.send("", cola, registro(id));
            hasta(() -> entregas.get() >= 2, 10000);
            assertEquals(1, eventos.findAll().stream().filter(e -> e.getEventId().equals(id)).count());
            assertTrue(procesador.procesarSiguiente()); assertFalse(procesador.procesarSiguiente());
            verify(email, times(1)).enviarCorreoVerificacion(anyString(), anyString(), anyString());
            container.stop(); assertEquals(0, mensajesListos(admin, cola));
        } finally { container.stop(); cf.destroy(); }
    }

    private SimpleMessageListenerContainer contenedor(CachingConnectionFactory cf, NotificacionListener listener, String cola) throws Exception {
        RabbitProperties properties = new RabbitProperties(); properties.getListener().getSimple().setAutoStartup(false);
        var factory = new RabbitMQConfig().rabbitListenerContainerFactory(
                new SimpleRabbitListenerContainerFactoryConfigurer(properties), cf, 10);
        factory.setRecoveryInterval(100L);
        var endpoint = new MethodRabbitListenerEndpoint(); endpoint.setId(UUID.randomUUID().toString());
        endpoint.setBean(listener); endpoint.setMethod(NotificacionListener.class.getMethod("procesarRegistroUsuario", Message.class));
        var methods = new DefaultMessageHandlerMethodFactory(); methods.afterPropertiesSet();
        endpoint.setMessageHandlerMethodFactory(methods); endpoint.setQueueNames(cola);
        return factory.createListenerContainer(endpoint);
    }

    private int mensajesListos(RabbitAdmin admin, String cola) {
        Properties p = admin.getQueueProperties(cola);
        return p == null ? -1 : ((Number) p.get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).intValue();
    }

    private void hasta(BooleanSupplier condicion, long millis) throws InterruptedException {
        long fin = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        while (!condicion.getAsBoolean() && System.nanoTime() < fin) Thread.sleep(20);
        assertTrue(condicion.getAsBoolean(), "No se completó la operación de infraestructura en el plazo de prueba.");
    }

    private void esperar(CountDownLatch latch) {
        try { if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Tiempo de prueba agotado"); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(); }
    }

    private void habilitar(String id) {
        tx.executeWithoutResult(status -> eventos.findById(id).orElseThrow().setProximoIntento(Instant.now().minusSeconds(1)));
    }

    private Message registro(String id) {
        var props = new MessageProperties(); props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setMessageId(id); if (id != null) props.setHeader("eventId", id);
        return new Message(JsonMapper.builder().build().writeValueAsBytes(new UsuarioRegistradoEvent(
                "prueba@example.com", id == null ? "Ana-" + UUID.randomUUID() : "Ana", "token-de-prueba")), props);
    }

    private ProcesadorNotificacion procesadorReal(int puerto) {
        return procesadorReal(puerto, false);
    }

    private ProcesadorNotificacion procesadorReal(int puerto, boolean plantillaRota) {
        var sender = new JavaMailSenderImpl(); sender.setHost("127.0.0.1"); sender.setPort(puerto);
        sender.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout", "300");
        sender.getJavaMailProperties().setProperty("mail.smtp.timeout", "300");
        sender.getJavaMailProperties().setProperty("mail.smtp.writetimeout", "300");
        var resolver = new ClassLoaderTemplateResolver(); resolver.setPrefix(plantillaRota ? "plantillas-inexistentes/" : "templates/"); resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8"); resolver.setTemplateMode("HTML");
        var engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        var realEmail = new EmailService(sender, engine);
        ReflectionTestUtils.setField(realEmail, "frontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(realEmail, "emailRemitente", "test@example.com");
        return new ProcesadorNotificacion(eventos, historiales, realEmail, 5, 60000, 300000);
    }

    private boolean procesarReal(ProcesadorNotificacion real) {
        return Boolean.TRUE.equals(tx.execute(status -> real.procesarSiguiente()));
    }
}
