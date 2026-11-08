package com.microservice.reservas.service;

import com.microservice.reservas.entity.Negocio;
import com.microservice.reservas.entity.Recurso;
import com.microservice.reservas.entity.Usuario;
import com.microservice.reservas.enums.EstadoAsociacion;
import com.microservice.reservas.enums.EstadoCuenta;
import com.microservice.reservas.exception.InvitacionException;
import com.microservice.reservas.repository.NegocioRepository;
import com.microservice.reservas.repository.RecursoRepository;
import com.microservice.reservas.repository.UsuarioRepository;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
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
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Solo admite una base local dedicada a pruebas. No usa la base de la aplicación. */
@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.datasource.hikari.transaction-isolation=TRANSACTION_READ_COMMITTED",
        "spring.datasource.hikari.connection-init-sql=SET statement_timeout = '15s'",
        "logging.level.root=WARN", "debug=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({RecursoService.class, NegocioService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnabledIfEnvironmentVariable(named = "AGENDAPP_TEST_POSTGRES_URL", matches = "jdbc:postgresql:.*")
class InvitacionPostgresTest {
    @Autowired private RecursoService service;
    @Autowired private RecursoRepository recursos;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private NegocioRepository negocios;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private DataSource dataSource;
    @MockitoBean private OutboxService outbox;

    @DynamicPropertySource
    static void baseAislada(DynamicPropertyRegistry registry) {
        String url = System.getenv("AGENDAPP_TEST_POSTGRES_URL");
        if (url == null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/agendapp_invitaciones_test")) {
            throw new IllegalArgumentException("Se requiere una base PostgreSQL local exclusiva: agendapp_invitaciones_test.");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void aceptarYRechazarConcurrentementeSoloConfirmanUnaDecision(boolean aceptarPrimero) throws Exception {
        String token = UUID.randomUUID().toString();
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
        Long id = transaccion.execute(tx -> {
            Usuario propietario = usuario("propietario-" + UUID.randomUUID() + "@example.com");
            Usuario empleado = usuario("empleado-" + UUID.randomUUID() + "@example.com");
            Negocio negocio = new Negocio();
            negocio.setAlias("prueba-" + UUID.randomUUID());
            negocio.setNombre("Negocio de prueba");
            negocio.setOwner(propietario);
            negocios.save(negocio);
            Recurso recurso = new Recurso();
            recurso.setNombre("Recurso de prueba");
            recurso.setNegocio(negocio);
            recurso.setUsuario(empleado);
            recurso.setEstadoAsociacion(EstadoAsociacion.PENDIENTE);
            recurso.setTokenInvitacion(token);
            recurso.setFechaExpiracionInvitacion(LocalDateTime.now().plusDays(7));
            return recursos.save(recurso).getId();
        });

        var primeraBloqueada = new CountDownLatch(1);
        var permitirCommit = new CountDownLatch(1);
        doAnswer(invocation -> {
            primeraBloqueada.countDown();
            if (!permitirCommit.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("La prueba no liberó la primera transacción a tiempo.");
            }
            return null;
        }).when(outbox).encolar(anyString(), anyString(), any(Object.class));

        var executor = Executors.newFixedThreadPool(2);
        try {
            var primera = executor.submit(() -> decidir(aceptarPrimero, token));
            assertTrue(primeraBloqueada.await(10, TimeUnit.SECONDS), "La primera decisión debe mantener el bloqueo hasta el commit.");
            var segunda = executor.submit(() -> decidir(!aceptarPrimero, token));

            // Comprobación en PostgreSQL: la segunda conexión espera un bloqueo real.
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            boolean esperaBloqueo = false;
            while (System.nanoTime() < limite) {
                esperaBloqueo = Boolean.TRUE.equals(jdbc.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM pg_stat_activity WHERE datname = current_database() "
                                + "AND wait_event_type = 'Lock' AND cardinality(pg_blocking_pids(pid)) > 0)", Boolean.class));
                if (esperaBloqueo) break;
                Thread.sleep(25);
            }
            assertTrue(esperaBloqueo, "PostgreSQL debe bloquear la segunda decisión sobre la misma fila.");
            assertFalse(segunda.isDone());
            permitirCommit.countDown();
            assertEquals(HttpStatus.OK, primera.get(10, TimeUnit.SECONDS));
            assertEquals(HttpStatus.NOT_FOUND, segunda.get(10, TimeUnit.SECONDS));

            transaccion.executeWithoutResult(tx -> {
                Recurso persistido = recursos.findById(id).orElseThrow();
                assertEquals(aceptarPrimero ? EstadoAsociacion.ACTIVO : EstadoAsociacion.RECHAZADO,
                        persistido.getEstadoAsociacion());
                assertEquals(aceptarPrimero, persistido.getUsuario() != null);
                assertNull(persistido.getTokenInvitacion());
                assertNull(persistido.getFechaExpiracionInvitacion());
            });
            verify(outbox, times(1)).encolar(anyString(), anyString(), any(Object.class));
        } finally {
            permitirCommit.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private HttpStatus decidir(boolean aceptar, String token) {
        try {
            if (aceptar) service.aceptarInvitacion(token);
            else service.rechazarInvitacion(token);
            return HttpStatus.OK;
        } catch (InvitacionException e) {
            return e.getStatus();
        }
    }

    private Usuario usuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPassword(UUID.randomUUID().toString());
        usuario.setEstadoCuenta(EstadoCuenta.ACTIVO);
        return usuarios.save(usuario);
    }
}
