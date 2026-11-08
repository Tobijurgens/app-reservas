package com.microservice.reservas.security;

import com.microservice.reservas.config.ApplicationConfig;
import com.microservice.reservas.config.SecurityConfig;
import com.microservice.reservas.controller.*;
import com.microservice.reservas.dto.request.HorarioSemanalDTO;
import com.microservice.reservas.dto.request.RecursoDTO;
import com.microservice.reservas.dto.request.ServicioDTO;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.enums.*;
import com.microservice.reservas.repository.*;
import com.microservice.reservas.service.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.microservice.reservas.service.OutboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Usa la cadena JWT, UserDetailsService, controladores y servicios reales. Solo simula la infraestructura. */
@WebMvcTest({RecursoController.class, ServicioController.class, TurnoController.class,
        AuthController.class, UsuarioController.class, NegocioController.class})
@Import({SecurityConfig.class, ApplicationConfig.class, JwtService.class, JwtAuthenticationFilter.class,
        RecursoService.class, ServicioService.class, TurnoService.class, AuthService.class,
        UsuarioService.class, NegocioService.class})
class ReservasSecurityTest {

    private static final String OWNER = "dueno@example.com";
    private static final String OTHER_OWNER = "otro-dueno@example.com";
    private static final String EMPLOYEE = "empleado@example.com";
    private static final String OTHER_EMPLOYEE = "otro-empleado@example.com";
    private static final String CLIENT = "cliente@example.com";
    private static final String TEST_SECRET = UUID.randomUUID().toString() + UUID.randomUUID();
    private static final String TEST_PASSWORD = UUID.randomUUID().toString();
    private static final String TEST_PASSWORD_HASH = new BCryptPasswordEncoder().encode(TEST_PASSWORD);
    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 1, 0, 0);

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> TEST_SECRET);
    }

    @Autowired private MockMvc mvc;
    @Autowired private JsonMapper json;
    @Autowired private JwtService jwtService;
    @MockitoBean private UsuarioRepository usuarios;
    @MockitoBean private RecursoRepository recursos;
    @MockitoBean private ServicioRepository servicios;
    @MockitoBean private NegocioRepository negocios;
    @MockitoBean private TurnoRepository turnos;
    @MockitoBean private TokenSeguridadRepository tokens;
    @MockitoBean private OutboxService outbox;

    private Map<String, Usuario> cuentas;
    private Negocio negocio;
    private Recurso recurso;
    private Servicio servicio;
    private Turno turno;

    @BeforeEach
    void prepararDatos() {
        cuentas = new HashMap<>();
        for (String email : List.of(OWNER, OTHER_OWNER, EMPLOYEE, OTHER_EMPLOYEE, CLIENT)) {
            Usuario usuario = new Usuario();
            usuario.setId((long) cuentas.size() + 1);
            usuario.setEmail(email);
            usuario.setNombre("Nombre");
            usuario.setApellido("Apellido");
            usuario.setTelefono("12345678");
            usuario.setEstadoCuenta(EstadoCuenta.ACTIVO);
            usuario.setPassword(TEST_PASSWORD_HASH);
            cuentas.put(email, usuario);
        }
        when(usuarios.findByEmail(anyString())).thenAnswer(c -> Optional.ofNullable(cuentas.get(c.getArgument(0))));
        when(usuarios.findByEmailAndEstadoCuenta(anyString(), any())).thenAnswer(c -> {
            Usuario usuario = cuentas.get(c.getArgument(0));
            return usuario != null && usuario.getEstadoCuenta() == c.getArgument(1) ? Optional.of(usuario) : Optional.empty();
        });
        when(usuarios.save(any())).thenAnswer(c -> {
            Usuario usuario = c.getArgument(0);
            if (usuario.getId() == null) usuario.setId(10L);
            return usuario;
        });

        negocio = negocio(1L, cuentas.get(OWNER));
        recurso = recurso(1L, negocio, cuentas.get(EMPLOYEE));
        Recurso ajeno = recurso(2L, negocio(2L, cuentas.get(OTHER_OWNER)), cuentas.get(OTHER_EMPLOYEE));
        Recurso otroEmpleado = recurso(3L, negocio, cuentas.get(OTHER_EMPLOYEE));
        Recurso inactivo = recurso(4L, negocio, cuentas.get(OTHER_EMPLOYEE));
        inactivo.setActivo(false);
        Map<Long, Recurso> porId = Map.of(1L, recurso, 2L, ajeno, 3L, otroEmpleado, 4L, inactivo);
        when(recursos.findByActivoTrue()).thenReturn(List.of(recurso, ajeno, otroEmpleado));
        when(recursos.findByNegocioIdAndActivoTrueOrderByNombreAsc(1L)).thenReturn(List.of(recurso, otroEmpleado));
        when(recursos.findByNegocioIdOrderByNombreAsc(1L)).thenReturn(List.of(recurso, otroEmpleado, inactivo));
        when(recursos.findByUsuarioEmailAndActivoTrueOrderByNombreAsc(anyString())).thenAnswer(c -> porId.values().stream()
                .filter(r -> r.getActivo() && r.getUsuario() != null && r.getUsuario().getEmail().equals(c.getArgument(0))).toList());
        when(recursos.existsByNegocioIdAndUsuarioEmailAndEstadoAsociacionAndActivoTrue(anyLong(), anyString(), any()))
                .thenAnswer(c -> porId.values().stream().anyMatch(r -> r.getNegocio().getId().equals(c.getArgument(0))
                        && PermisosRecurso.esEmpleadoActivo(r, c.getArgument(1))));
        when(recursos.findByIdAndActivoTrue(anyLong())).thenAnswer(c -> Optional.ofNullable(porId.get(c.getArgument(0))));
        when(recursos.findByIdActivoParaActualizar(anyLong())).thenAnswer(c -> Optional.ofNullable(porId.get(c.getArgument(0))));
        when(recursos.findByTokenInvitacion(anyString())).thenAnswer(c -> porId.values().stream()
                .filter(r -> c.getArgument(0).equals(r.getTokenInvitacion())).findFirst());
        when(recursos.findByTokenInvitacionParaActualizar(anyString())).thenAnswer(c -> porId.values().stream()
                .filter(r -> c.getArgument(0).equals(r.getTokenInvitacion())).findFirst());
        when(recursos.findById(anyLong())).thenAnswer(c -> Optional.ofNullable(porId.get(c.getArgument(0))));
        when(recursos.save(any())).thenAnswer(c -> {
            Recurso r = c.getArgument(0);
            if (r.getId() == null) r.setId(4L);
            return r;
        });
        when(negocios.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(negocio));
        when(negocios.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(ajeno.getNegocio()));
        when(negocios.findByAliasAndActivoTrue("local-1")).thenReturn(Optional.of(negocio));
        when(negocios.findByActivoTrue()).thenReturn(List.of(negocio, ajeno.getNegocio()));
        when(negocios.findByOwnerIdAndActivoTrue(cuentas.get(OWNER).getId())).thenReturn(List.of(negocio));

        servicio = new Servicio();
        servicio.setId(1L);
        servicio.setNombre("Servicio");
        servicio.setPrecio(100.0);
        servicio.setDuracionMinutos(30);
        servicio.setRecurso(recurso);
        when(servicios.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(servicio));
        when(servicios.findById(1L)).thenReturn(Optional.of(servicio));
        when(servicios.save(any())).thenAnswer(c -> c.getArgument(0));
        when(servicios.findByActivoTrue()).thenReturn(List.of(servicio));
        when(servicios.findByRecursoIdAndActivoTrue(1L)).thenReturn(List.of(servicio));

        turno = new Turno(1L, START.plusYears(2), START.plusYears(2).plusMinutes(30),
                EstadoTurno.CONFIRMADO, cuentas.get(CLIENT), recurso, servicio);
        when(turnos.findById(1L)).thenReturn(Optional.of(turno));
        when(turnos.findByIdParaActualizar(1L)).thenReturn(Optional.of(turno));
        when(turnos.save(any())).thenAnswer(c -> c.getArgument(0));
        when(turnos.findByNegocioIdDesc(eq(1L), any())).thenReturn(Page.empty());
        when(turnos.findByRecursoIdDesc(eq(1L), any())).thenReturn(Page.empty());
    }

    @Test
    void cuentaActivaConJwtValidoAccede() throws Exception {
        mvc.perform(conToken(get("/api/usuarios/perfil"), CLIENT)).andExpect(status().isOk());
    }

    @Test
    void mismoJwtDejaDeFuncionarCuandoSeSuspendeLaCuenta() throws Exception {
        String jwt = jwtService.generateToken(cuentas.get(CLIENT));
        mvc.perform(get("/api/usuarios/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk());
        cuentas.get(CLIENT).setEstadoCuenta(EstadoCuenta.SUSPENDIDO);
        mvc.perform(get("/api/usuarios/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    @Test
    void bajaLogicaRealImpideReutilizarElToken() throws Exception {
        Usuario cliente = cuentas.get(CLIENT);
        String jwt = jwtService.generateToken(cliente);
        when(usuarios.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        mvc.perform(delete("/api/usuarios/{id}", cliente.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)).andExpect(status().isOk());
        assertEquals(EstadoCuenta.SUSPENDIDO, cliente.getEstadoCuenta());
        mvc.perform(get("/api/usuarios/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cuentaPendienteNoSeAutenticaConJwt() throws Exception {
        cuentas.get(CLIENT).setEstadoCuenta(EstadoCuenta.PENDIENTE_VERIFICACION);
        mvc.perform(conToken(get("/api/usuarios/perfil"), CLIENT)).andExpect(status().isUnauthorized());
    }

    @Test
    void cuentaInexistenteNoSeAutentica() throws Exception {
        String jwt = jwtService.generateToken(cuentas.get(CLIENT));
        cuentas.remove(CLIENT);
        mvc.perform(get("/api/usuarios/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinJwtNoSeAccedeAUnEndpointPrivado() throws Exception {
        mvc.perform(get("/api/usuarios/perfil")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalido", "null", "undefined", ""})
    void tokenInvalidoSeRechaza(String jwt) throws Exception {
        mvc.perform(get("/api/usuarios/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoSeRechaza() throws Exception {
        String jwt = Jwts.builder().subject(CLIENT).expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes())).compact();
        mvc.perform(get("/api/usuarios/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void firmaDeOtraClaveSeRechaza() throws Exception {
        String jwt = Jwts.builder().subject(CLIENT).expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Jwts.SIG.HS512.key().build()).compact();
        mvc.perform(get("/api/usuarios/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void consultaPublicaSigueDisponibleConCuentaSuspendida() throws Exception {
        cuentas.get(CLIENT).setEstadoCuenta(EstadoCuenta.SUSPENDIDO);
        mvc.perform(conToken(get("/api/servicios/recurso/1"), CLIENT)).andExpect(status().isOk());
    }

    @Test
    void propietarioPuedeEditarSuRecursoConInvitacionPendiente() throws Exception {
        recurso.setEstadoAsociacion(EstadoAsociacion.PENDIENTE);
        RecursoDTO dto = edicion();
        dto.setNombre("Nuevo nombre");
        mvc.perform(conToken(body(put("/api/recursos/1"), dto), OWNER)).andExpect(status().isOk());
        assertEquals("Nuevo nombre", recurso.getNombre());
        assertEquals(EstadoAsociacion.PENDIENTE, recurso.getEstadoAsociacion());
    }

    @Test
    void propietarioPuedeReasignarEmpleado() throws Exception {
        RecursoDTO dto = edicion();
        dto.setEmailUsuario(OTHER_EMPLOYEE);
        mvc.perform(conToken(body(put("/api/recursos/1"), dto), OWNER)).andExpect(status().isOk());
        assertSame(cuentas.get(OTHER_EMPLOYEE), recurso.getUsuario());
        assertEquals(EstadoAsociacion.PENDIENTE, recurso.getEstadoAsociacion());
        assertNotNull(recurso.getTokenInvitacion());
        verify(outbox).encolar(eq("notificaciones.exchange"), eq("invitacion.email.routing.key"), any(Object.class));
    }

    @Test
    void propietarioPuedeCrearRecursoPropio() throws Exception {
        mvc.perform(conToken(body(post("/api/recursos"), edicion()), OWNER)).andExpect(status().isOk());
        verify(recursos).save(any());
    }

    @Test
    void propietarioAjenoNoPuedeCrearNiModificarRecursos() throws Exception {
        mvc.perform(conToken(body(post("/api/recursos"), edicion()), OTHER_OWNER)).andExpect(status().isForbidden());
        mvc.perform(conToken(body(put("/api/recursos/1"), edicion()), OTHER_OWNER))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        verify(recursos, never()).save(any());
    }

    @Test
    void empleadoActivoEditaDescripcionYHorariosSinCambiarAsociacion() throws Exception {
        RecursoDTO dto = edicion();
        dto.setDescripcion("Nueva descripcion");
        dto.getHorarios().get(0).setHoraInicio(LocalTime.of(10, 0));
        mvc.perform(conToken(body(put("/api/recursos/1"), dto), EMPLOYEE)).andExpect(status().isOk());
        assertEquals("Nueva descripcion", recurso.getDescripcion());
        assertEquals(LocalTime.of(10, 0), recurso.getHorarios().get(0).getHoraInicio());
        assertSame(cuentas.get(EMPLOYEE), recurso.getUsuario());
        assertEquals(EstadoAsociacion.ACTIVO, recurso.getEstadoAsociacion());
        verifyNoInteractions(outbox);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoAsociacion.class, names = {"PENDIENTE", "RECHAZADO"})
    void empleadoSinAsociacionAceptadaNoEditaRecurso(EstadoAsociacion estado) throws Exception {
        recurso.setEstadoAsociacion(estado);
        mvc.perform(conToken(body(put("/api/recursos/1"), edicion()), EMPLOYEE)).andExpect(status().isForbidden());
        verify(recursos, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {OTHER_EMPLOYEE, ""})
    void empleadoNoPuedeReasignarNiDesasignarRecurso(String email) throws Exception {
        RecursoDTO dto = edicion();
        dto.setEmailUsuario(email);
        mvc.perform(conToken(body(put("/api/recursos/1"), dto), EMPLOYEE)).andExpect(status().isForbidden());
        assertSame(cuentas.get(EMPLOYEE), recurso.getUsuario());
        verify(recursos, never()).save(any());
        verifyNoInteractions(outbox);
    }

    @Test
    void omitirEmailNoPermiteDesasignarse() throws Exception {
        RecursoDTO dto = edicion();
        dto.setEmailUsuario(null);
        mvc.perform(conToken(body(put("/api/recursos/1"), dto), EMPLOYEE)).andExpect(status().isForbidden());
        verify(recursos, never()).save(any());
    }

    @Test
    void empleadoNoPuedeRenombrarRecurso() throws Exception {
        RecursoDTO dto = edicion();
        dto.setNombre("Nombre manipulado");
        mvc.perform(conToken(body(put("/api/recursos/1"), dto), EMPLOYEE)).andExpect(status().isForbidden());
        assertEquals("Recurso", recurso.getNombre());
        verify(recursos, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {OWNER, EMPLOYEE})
    void noSeTransfiereRecursoAOtroNegocio(String email) throws Exception {
        RecursoDTO dto = edicion();
        dto.setIdNegocio(2L);
        mvc.perform(conToken(body(put("/api/recursos/1"), dto), email)).andExpect(status().isForbidden());
        assertSame(negocio, recurso.getNegocio());
        verify(recursos, never()).save(any());
    }

    @Test
    void empleadoNoAdministraRecursoAjenoNiEliminaElPropio() throws Exception {
        mvc.perform(conToken(body(put("/api/recursos/2"), edicion()), EMPLOYEE)).andExpect(status().isForbidden());
        mvc.perform(conToken(delete("/api/recursos/1"), EMPLOYEE)).andExpect(status().isForbidden());
        verify(recursos, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {OWNER, EMPLOYEE})
    void propietarioYEmpleadoActivoGestionanServicios(String email) throws Exception {
        mvc.perform(conToken(body(post("/api/servicios"), servicioDTO()), email)).andExpect(status().isOk());
        mvc.perform(conToken(body(put("/api/servicios/1"), servicioDTO()), email)).andExpect(status().isOk());
        mvc.perform(conToken(delete("/api/servicios/1"), email)).andExpect(status().isOk());
        assertFalse(servicio.getActivo());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoAsociacion.class, names = {"PENDIENTE", "RECHAZADO"})
    void empleadoPendienteORechazadoNoGestionaServicios(EstadoAsociacion estado) throws Exception {
        recurso.setEstadoAsociacion(estado);
        mvc.perform(conToken(body(post("/api/servicios"), servicioDTO()), EMPLOYEE)).andExpect(status().isForbidden());
        mvc.perform(conToken(body(put("/api/servicios/1"), servicioDTO()), EMPLOYEE)).andExpect(status().isForbidden());
        mvc.perform(conToken(delete("/api/servicios/1"), EMPLOYEE)).andExpect(status().isForbidden());
        verify(servicios, never()).save(any());
    }

    @Test
    void clienteNoPuedeAdministrarServicios() throws Exception {
        mvc.perform(conToken(body(post("/api/servicios"), servicioDTO()), CLIENT)).andExpect(status().isForbidden());
        mvc.perform(conToken(body(put("/api/servicios/1"), servicioDTO()), CLIENT)).andExpect(status().isForbidden());
        mvc.perform(conToken(delete("/api/servicios/1"), CLIENT)).andExpect(status().isForbidden());
        verify(servicios, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {OWNER, EMPLOYEE})
    void propietarioYEmpleadoActivoConsultanAgendaHistorialYReportePropios(String email) throws Exception {
        mvc.perform(conToken(get("/api/turnos/agenda/1"), email)).andExpect(status().isOk());
        mvc.perform(conToken(get("/api/turnos/recurso/1/historial"), email)).andExpect(status().isOk());
        mvc.perform(conToken(reporte(1L, 1L), email)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoAsociacion.class, names = {"PENDIENTE", "RECHAZADO"})
    void empleadoPendienteORechazadoNoConsultaAgendaHistorialNiReportes(EstadoAsociacion estado) throws Exception {
        recurso.setEstadoAsociacion(estado);
        mvc.perform(conToken(get("/api/turnos/agenda/1"), EMPLOYEE)).andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/turnos/recurso/1/historial"), EMPLOYEE)).andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/turnos/recurso/1/disponibilidad").param("fecha", "2026-11-05"), EMPLOYEE))
                .andExpect(status().isForbidden());
        mvc.perform(conToken(reporte(1L, 1L), EMPLOYEE)).andExpect(status().isForbidden());
        verify(turnos, never()).findByRecursoId(anyLong());
        verify(turnos, never()).findTurnosParaReportePorRecurso(anyLong(), anyLong(), any(), any());
    }

    @Test
    void soloPropietarioAccedeAHistorialYReporteGlobales() throws Exception {
        mvc.perform(conToken(get("/api/turnos/negocio/1"), OWNER)).andExpect(status().isOk());
        mvc.perform(conToken(reporte(1L, null), OWNER)).andExpect(status().isOk());
        mvc.perform(conToken(get("/api/turnos/negocio/1"), EMPLOYEE)).andExpect(status().isForbidden());
        mvc.perform(conToken(reporte(1L, null), EMPLOYEE)).andExpect(status().isForbidden());
    }

    @Test
    void empleadoNoConsultaReportesPrivadosAjenosNiCruzaNegocios() throws Exception {
        mvc.perform(conToken(reporte(1L, 3L), EMPLOYEE)).andExpect(status().isForbidden());
        mvc.perform(conToken(reporte(2L, 1L), EMPLOYEE)).andExpect(status().isForbidden());
        mvc.perform(conToken(reporte(1L, 2L), OWNER)).andExpect(status().isForbidden());
        verify(turnos, never()).findTurnosParaReportePorRecurso(anyLong(), anyLong(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {OWNER, EMPLOYEE, CLIENT})
    void relacionesAutorizadasConservanGestionDeTurnos(String email) throws Exception {
        mvc.perform(conToken(get("/api/turnos/1"), email)).andExpect(status().isOk());
        mvc.perform(conToken(patch("/api/turnos/1/estado").param("nuevoEstado", "CANCELADO"), email))
                .andExpect(status().isOk());
        assertEquals(EstadoTurno.CANCELADO, turno.getEstado());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoAsociacion.class, names = {"PENDIENTE", "RECHAZADO"})
    void empleadoSinAsociacionAceptadaNoGestionaTurnosAjenos(EstadoAsociacion estado) throws Exception {
        recurso.setEstadoAsociacion(estado);
        mvc.perform(conToken(get("/api/turnos/1"), EMPLOYEE)).andExpect(status().isForbidden());
        mvc.perform(conToken(patch("/api/turnos/1/estado").param("nuevoEstado", "CANCELADO"), EMPLOYEE))
                .andExpect(status().isForbidden());
        mvc.perform(conToken(delete("/api/turnos/1"), EMPLOYEE)).andExpect(status().isForbidden());
        assertEquals(EstadoTurno.CONFIRMADO, turno.getEstado());
        verify(turnos, never()).save(any());
        verifyNoInteractions(outbox);
    }

    @Test
    void empleadoPendienteConservaSusDerechosComoCliente() throws Exception {
        recurso.setEstadoAsociacion(EstadoAsociacion.PENDIENTE);
        turno.setCliente(cuentas.get(EMPLOYEE));
        mvc.perform(conToken(delete("/api/turnos/1"), EMPLOYEE)).andExpect(status().isOk());
        assertEquals(EstadoTurno.CANCELADO, turno.getEstado());
    }

    @Test
    void loginActivoFuncionaAunqueLlegueUnBearerObsoleto() throws Exception {
        mvc.perform(post("/api/auth/login").servletPath("/api/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer obsoleto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + CLIENT + "\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void registroSiguePublicoYEnviaEventoDeVerificacion() throws Exception {
        mvc.perform(post("/api/auth/register").servletPath("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"apellido\":\"Perez\",\"email\":\"nueva@example.com\","
                                + "\"telefono\":\"12345678\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isOk());
        verify(outbox).encolar(eq("notificaciones.exchange"), eq("registro.email.routing.key"), any(Object.class));
    }

    @Test
    void verificacionSigueActivandoLaCuenta() throws Exception {
        Usuario cliente = cuentas.get(CLIENT);
        cliente.setEstadoCuenta(EstadoCuenta.PENDIENTE_VERIFICACION);
        TokenSeguridad token = new TokenSeguridad();
        token.setToken("verificacion-de-prueba");
        token.setUsuario(cliente);
        token.setTipo(TipoToken.VERIFICACION);
        token.setFechaExpiracion(LocalDateTime.now().plusMinutes(5));
        when(tokens.findByToken(token.getToken())).thenReturn(Optional.of(token));
        mvc.perform(get("/api/auth/verificar").servletPath("/api/auth/verificar").param("token", token.getToken()))
                .andExpect(status().isOk());
        assertTrue(cliente.isEnabled());
        verify(tokens).delete(token);
    }

    @Test
    void perfilPropioConservaDatosSinExponerCredenciales() throws Exception {
        mvc.perform(conToken(get("/api/usuarios/perfil").param("email", OWNER), CLIENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cuentas.get(CLIENT).getId()))
                .andExpect(jsonPath("$.email").value(CLIENT))
                .andExpect(jsonPath("$.nombre").value("Nombre"))
                .andExpect(jsonPath("$.apellido").value("Apellido"))
                .andExpect(jsonPath("$.telefono").value("12345678"))
                .andExpect(jsonPath("$.estadoCuenta").value("ACTIVO"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.authorities").doesNotExist());
    }

    @Test
    void busquedasAntiguasPermitenUnicamenteElPerfilPropio() throws Exception {
        mvc.perform(conToken(get("/api/usuarios/{id}", cuentas.get(CLIENT).getId()), CLIENT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(CLIENT));
        mvc.perform(conToken(get("/api/usuarios/email/{email}", CLIENT), CLIENT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(CLIENT));
    }

    @ParameterizedTest
    @ValueSource(strings = {OWNER, EMPLOYEE, CLIENT})
    void nadiePuedeConsultarDatosPersonalesDeOtraCuenta(String email) throws Exception {
        mvc.perform(conToken(get("/api/usuarios/{id}", cuentas.get(OTHER_OWNER).getId()), email))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.email").doesNotExist());
        mvc.perform(conToken(get("/api/usuarios/email/{email}", OTHER_OWNER), email))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.telefono").doesNotExist());
        mvc.perform(conToken(get("/api/usuarios/email/inexistente@example.com"), email))
                .andExpect(status().isForbidden());
        verify(usuarios, never()).findByIdAndEstadoCuenta(anyLong(), any());
        verify(usuarios, never()).findByEmailAndEstadoCuenta(OTHER_OWNER, EstadoCuenta.ACTIVO);
    }

    @ParameterizedTest
    @ValueSource(strings = {OWNER, EMPLOYEE, CLIENT})
    void listadoGlobalDeUsuariosNoEstaAutorizado(String email) throws Exception {
        mvc.perform(conToken(get("/api/usuarios"), email)).andExpect(status().isForbidden());
        verify(usuarios, never()).findByEstadoCuenta(any());
    }

    @Test
    void edicionDePerfilUsaPrincipalYConservaElEmail() throws Exception {
        mvc.perform(conToken(put("/api/usuarios/perfil").param("email", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Actualizado\",\"apellido\":\"Apellido\",\"telefono\":\"87654321\"}"), CLIENT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(CLIENT))
                .andExpect(jsonPath("$.nombre").value("Actualizado"));
        assertEquals("Nombre", cuentas.get(OWNER).getNombre());
        assertEquals("12345678", cuentas.get(OWNER).getTelefono());
        assertEquals(CLIENT, cuentas.get(CLIENT).getEmail());
    }

    @Test
    void visitanteConsultaNegocioPublicoSinCorreosPrivadosNiTokens() throws Exception {
        mvc.perform(get("/api/negocios/publico/local-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.alias").value("local-1"))
                .andExpect(jsonPath("$.nombre").value("Local 1"))
                .andExpect(jsonPath("$.direccion").value("Direccion comercial"))
                .andExpect(jsonPath("$.telefono").value("011-publico"))
                .andExpect(jsonPath("$.emailContacto").value("contacto@local.example"))
                .andExpect(jsonPath("$.horarios[0].diaSemana").value("MONDAY"))
                .andExpect(jsonPath("$.horarios[0].horaInicio").exists())
                .andExpect(jsonPath("$.emailOwner").doesNotExist())
                .andExpect(jsonPath("$.owner").doesNotExist())
                .andExpect(jsonPath("$.propietario").doesNotExist())
                .andExpect(jsonPath("$.tokenInvitacion").doesNotExist());
    }

    @Test
    void visitanteConsultaRecursosYServiciosSinDatosAdministrativos() throws Exception {
        mvc.perform(get("/api/recursos/negocio/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Recurso"))
                .andExpect(jsonPath("$[0].negocioId").value(1))
                .andExpect(jsonPath("$[0].emailUsuario").doesNotExist())
                .andExpect(jsonPath("$[1].emailUsuario").doesNotExist())
                .andExpect(jsonPath("$[0].estadoAsociacion").doesNotExist())
                .andExpect(jsonPath("$[0].horarios").doesNotExist())
                .andExpect(jsonPath("$[0].usuario").doesNotExist())
                .andExpect(jsonPath("$[0].propietario").doesNotExist())
                .andExpect(jsonPath("$[0].tokenInvitacion").doesNotExist());
        mvc.perform(get("/api/servicios/recurso/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Servicio"))
                .andExpect(jsonPath("$[0].recursoId").value(1))
                .andExpect(jsonPath("$[0].precio").value(100.0))
                .andExpect(jsonPath("$[0].duracionMinutos").value(30))
                .andExpect(jsonPath("$[0].recurso").doesNotExist())
                .andExpect(jsonPath("$[0].emailUsuario").doesNotExist());
    }

    @Test
    void listadosGeneralesTampocoExponenDTOAdministrativos() throws Exception {
        mvc.perform(conToken(get("/api/negocios"), CLIENT)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].emailOwner").doesNotExist())
                .andExpect(jsonPath("$[0].propietario").doesNotExist());
        mvc.perform(conToken(get("/api/recursos"), CLIENT)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].emailUsuario").doesNotExist())
                .andExpect(jsonPath("$[0].estadoAsociacion").doesNotExist())
                .andExpect(jsonPath("$[0].horarios").doesNotExist());
    }

    @Test
    void negocioInactivoNoPublicaRecursosNiServicios() throws Exception {
        negocio.setActivo(false);
        when(negocios.findByAliasAndActivoTrue("local-1")).thenReturn(Optional.empty());
        when(negocios.findByIdAndActivoTrue(1L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/negocios/publico/local-1")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/recursos/negocio/1")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/servicios/recurso/1")).andExpect(status().is4xxClientError());
        mvc.perform(conToken(get("/api/recursos"), CLIENT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(conToken(get("/api/servicios"), CLIENT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void propietarioRecibeRecursosAdministrativosPropiosIncluidosInactivos() throws Exception {
        mvc.perform(conToken(get("/api/recursos/negocio/1/todos"), OWNER))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].emailUsuario").value(EMPLOYEE))
                .andExpect(jsonPath("$[0].estadoAsociacion").value("ACTIVO"))
                .andExpect(jsonPath("$[0].propietario").value(true))
                .andExpect(jsonPath("$[0].horarios").isArray())
                .andExpect(jsonPath("$[0].tokenInvitacion").doesNotExist())
                .andExpect(jsonPath("$[2].activo").value(false));
        mvc.perform(conToken(get("/api/negocios/1"), OWNER))
                .andExpect(status().isOk()).andExpect(jsonPath("$.propietario").value(true))
                .andExpect(jsonPath("$.emailOwner").doesNotExist());
        mvc.perform(conToken(get("/api/negocios/mis-negocios"), OWNER))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].propietario").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {OTHER_OWNER, CLIENT})
    void otrasCuentasNoConsultanAdministracionDelNegocio(String email) throws Exception {
        mvc.perform(conToken(get("/api/negocios/1"), email)).andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/recursos/negocio/1/todos"), email)).andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/recursos/1"), email)).andExpect(status().isForbidden());
    }

    @Test
    void empleadoActivoSoloRecibeSuRecursoAdministrativo() throws Exception {
        mvc.perform(conToken(get("/api/recursos/negocio/1/todos"), EMPLOYEE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].emailUsuario").value(EMPLOYEE))
                .andExpect(jsonPath("$[0].propietario").value(false));
        mvc.perform(conToken(get("/api/recursos/1"), EMPLOYEE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estadoAsociacion").value("ACTIVO"));
        mvc.perform(conToken(get("/api/recursos/3"), EMPLOYEE)).andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/negocios/1"), EMPLOYEE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.propietario").value(false))
                .andExpect(jsonPath("$.emailOwner").doesNotExist());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoAsociacion.class, names = {"PENDIENTE", "RECHAZADO"})
    void empleadoSinAceptarNoConsultaInformacionAdministrativa(EstadoAsociacion estado) throws Exception {
        recurso.setEstadoAsociacion(estado);
        mvc.perform(conToken(get("/api/recursos/1"), EMPLOYEE)).andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/recursos/negocio/1/todos"), EMPLOYEE)).andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/negocios/1"), EMPLOYEE)).andExpect(status().isForbidden());
    }

    @Test
    void misAsignacionesPermiteMostrarLaInvitacionSinExponerCompaneros() throws Exception {
        recurso.setEstadoAsociacion(EstadoAsociacion.PENDIENTE);
        mvc.perform(conToken(get("/api/recursos/mis-asignaciones").param("email", OTHER_EMPLOYEE), EMPLOYEE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].negocioId").value(1))
                .andExpect(jsonPath("$[0].nombreNegocio").value("Local 1"))
                .andExpect(jsonPath("$[0].emailUsuario").value(EMPLOYEE))
                .andExpect(jsonPath("$[0].estadoAsociacion").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].propietario").value(false))
                .andExpect(jsonPath("$[0].tokenInvitacion").doesNotExist());
        mvc.perform(conToken(get("/api/recursos/mis-asignaciones"), CLIENT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/usuarios/perfil", "/api/negocios/1", "/api/recursos/1", "/api/recursos/negocio/1/todos", "/api/recursos/mis-asignaciones"})
    void consultasPrivadasNoAceptanVisitantes(String endpoint) throws Exception {
        mvc.perform(get(endpoint)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/recursos/invitacion", "/api/recursos/invitacion/aceptar", "/api/recursos/invitacion/rechazar"})
    void abrirInvitacionInclusoEnlacesAntiguosSoloConsulta(String endpoint) throws Exception {
        String token = invitacionPendiente();
        mvc.perform(get(endpoint).param("token", token)).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.nombreNegocio").value("Local 1"))
                .andExpect(jsonPath("$.nombreRecurso").value("Recurso"))
                .andExpect(jsonPath("$.fechaExpiracion").exists())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.tokenInvitacion").doesNotExist())
                .andExpect(jsonPath("$.emailUsuario").doesNotExist());
        assertEquals(EstadoAsociacion.PENDIENTE, recurso.getEstadoAsociacion());
        assertEquals(token, recurso.getTokenInvitacion());
        verify(recursos, never()).save(any());
        verifyNoInteractions(outbox);
    }

    @Test
    void aceptarSoloPorPostHabilitaGestionYNoCambiaOtraAsociacion() throws Exception {
        String token = invitacionPendiente();
        mvc.perform(conToken(get("/api/recursos/1"), EMPLOYEE)).andExpect(status().isForbidden());
        mvc.perform(body(post("/api/recursos/invitacion/aceptar").param("recursoId", "3"), Map.of("token", token)))
                .andExpect(status().isOk());
        assertEquals(EstadoAsociacion.ACTIVO, recurso.getEstadoAsociacion());
        assertEquals(EMPLOYEE, recurso.getUsuario().getEmail());
        assertNull(recurso.getTokenInvitacion());
        assertNull(recurso.getFechaExpiracionInvitacion());
        mvc.perform(conToken(get("/api/recursos/1"), EMPLOYEE)).andExpect(status().isOk());
        mvc.perform(conToken(body(put("/api/recursos/1"), edicion()), EMPLOYEE)).andExpect(status().isOk());
        mvc.perform(conToken(get("/api/recursos/3"), EMPLOYEE)).andExpect(status().isForbidden());
        verify(outbox).encolar(eq("notificaciones.exchange"), eq("avisos.internos.routing.key"),
                any(com.microservice.reservas.dto.event.AvisoInternoEvent.class));
    }

    @Test
    void rechazarSoloPorPostMantienePropietarioYNoHabilitaEmpleado() throws Exception {
        String token = invitacionPendiente();
        mvc.perform(body(post("/api/recursos/invitacion/rechazar"), Map.of("token", token))).andExpect(status().isOk());
        assertEquals(EstadoAsociacion.RECHAZADO, recurso.getEstadoAsociacion());
        assertNull(recurso.getUsuario());
        assertNull(recurso.getTokenInvitacion());
        assertNull(recurso.getFechaExpiracionInvitacion());
        mvc.perform(conToken(get("/api/recursos/1"), EMPLOYEE)).andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/recursos/1"), OWNER)).andExpect(status().isOk());
        verify(outbox).encolar(eq("notificaciones.exchange"), eq("avisos.internos.routing.key"),
                any(com.microservice.reservas.dto.event.AvisoInternoEvent.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"aceptar", "rechazar"})
    void decisionConsumeTokenYNoPermiteRepetirNiRevertir(String decision) throws Exception {
        String token = invitacionPendiente();
        mvc.perform(body(post("/api/recursos/invitacion/" + decision), Map.of("token", token))).andExpect(status().isOk());
        EstadoAsociacion resultado = recurso.getEstadoAsociacion();
        for (String otraDecision : List.of("aceptar", "rechazar")) {
            mvc.perform(body(post("/api/recursos/invitacion/" + otraDecision), Map.of("token", token)))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/recursos/invitacion").param("token", token)).andExpect(status().isNotFound());
        assertEquals(resultado, recurso.getEstadoAsociacion());
        verify(recursos, times(1)).save(recurso);
        verify(outbox, times(1)).encolar(anyString(), anyString(), any(Object.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"aceptar", "rechazar"})
    void tokensInvalidosOVencidosOAntiguosNoSeProcesan(String decision) throws Exception {
        String token = invitacionPendiente();
        mvc.perform(body(post("/api/recursos/invitacion/" + decision), Map.of("token", UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
        recurso.setFechaExpiracionInvitacion(LocalDateTime.now().minusSeconds(1));
        mvc.perform(body(post("/api/recursos/invitacion/" + decision), Map.of("token", token)))
                .andExpect(status().isGone()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("venció")));
        mvc.perform(get("/api/recursos/invitacion").param("token", token)).andExpect(status().isGone());
        recurso.setFechaExpiracionInvitacion(null);
        mvc.perform(body(post("/api/recursos/invitacion/" + decision), Map.of("token", token))).andExpect(status().isGone());
        mvc.perform(get("/api/recursos/invitacion").param("token", token)).andExpect(status().isGone());
        assertEquals(EstadoAsociacion.PENDIENTE, recurso.getEstadoAsociacion());
        verify(recursos, never()).save(any());
        verifyNoInteractions(outbox);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVO", "RECHAZADO", "RECURSO_INACTIVO", "NEGOCIO_INACTIVO", "USUARIO_SUSPENDIDO", "SIN_USUARIO"})
    void tokenResidualNoProcesaUnaAsociacionNoDisponible(String caso) throws Exception {
        String token = invitacionPendiente();
        switch (caso) {
            case "ACTIVO" -> recurso.setEstadoAsociacion(EstadoAsociacion.ACTIVO);
            case "RECHAZADO" -> recurso.setEstadoAsociacion(EstadoAsociacion.RECHAZADO);
            case "RECURSO_INACTIVO" -> recurso.setActivo(false);
            case "NEGOCIO_INACTIVO" -> negocio.setActivo(false);
            case "USUARIO_SUSPENDIDO" -> cuentas.get(EMPLOYEE).setEstadoCuenta(EstadoCuenta.SUSPENDIDO);
            case "SIN_USUARIO" -> recurso.setUsuario(null);
        }
        mvc.perform(get("/api/recursos/invitacion").param("token", token)).andExpect(status().isNotFound());
        for (String decision : List.of("aceptar", "rechazar")) {
            mvc.perform(body(post("/api/recursos/invitacion/" + decision), Map.of("token", token)))
                    .andExpect(status().isNotFound());
        }
        verify(recursos, never()).save(any());
        verifyNoInteractions(outbox);
    }

    @ParameterizedTest
    @ValueSource(strings = {"aceptar", "rechazar"})
    void decisionesExigenTokenEnElCuerpo(String decision) throws Exception {
        String token = invitacionPendiente();
        mvc.perform(body(post("/api/recursos/invitacion/" + decision).param("token", token), Map.of()))
                .andExpect(status().isBadRequest());
        mvc.perform(body(post("/api/recursos/invitacion/" + decision), Map.of("token", " ")))
                .andExpect(status().isBadRequest());
        verify(recursos, never()).save(any());
    }

    @Test
    void tokenDeVerificacionNoProcesaInvitacionYTokenDeInvitacionNoVerificaCuenta() throws Exception {
        String invitacion = invitacionPendiente();
        String verificacion = UUID.randomUUID().toString();
        TokenSeguridad token = new TokenSeguridad();
        token.setToken(verificacion);
        token.setTipo(TipoToken.VERIFICACION);
        token.setUsuario(cuentas.get(CLIENT));
        token.setFechaExpiracion(LocalDateTime.now().plusMinutes(30));
        when(tokens.findByToken(verificacion)).thenReturn(Optional.of(token));
        mvc.perform(body(post("/api/recursos/invitacion/aceptar"), Map.of("token", verificacion)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/auth/verificar").param("token", invitacion)).andExpect(status().isBadRequest());
        assertEquals(EstadoAsociacion.PENDIENTE, recurso.getEstadoAsociacion());
        verify(usuarios, never()).save(any());
        verify(recursos, never()).save(any());
    }

    @Test
    void tokenDeRecuperacionNoVerificaCuenta() throws Exception {
        TokenSeguridad token = new TokenSeguridad();
        token.setToken(UUID.randomUUID().toString());
        token.setTipo(TipoToken.RECUPERACION);
        token.setUsuario(cuentas.get(CLIENT));
        token.setFechaExpiracion(LocalDateTime.now().plusMinutes(30));
        when(tokens.findByToken(token.getToken())).thenReturn(Optional.of(token));
        mvc.perform(get("/api/auth/verificar").param("token", token.getToken())).andExpect(status().isBadRequest());
        verify(usuarios, never()).save(any());
    }

    @Test
    void creacionEmiteTokenAleatorioConSieteDiasPorRabbitSinExponerloEnDto() throws Exception {
        var captor = org.mockito.ArgumentCaptor.forClass(com.microservice.reservas.dto.event.InvitacionEmpleadoEvent.class);
        LocalDateTime antes = LocalDateTime.now();
        mvc.perform(conToken(body(post("/api/recursos"), edicion()), OWNER))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estadoAsociacion").value("PENDIENTE"))
                .andExpect(jsonPath("$.tokenInvitacion").doesNotExist());
        verify(outbox).encolar(eq("notificaciones.exchange"), eq("invitacion.email.routing.key"), captor.capture());
        var evento = captor.getValue();
        assertEquals(EMPLOYEE, evento.getEmailDestino());
        assertEquals(4, UUID.fromString(evento.getToken()).version());
        var guardado = org.mockito.ArgumentCaptor.forClass(Recurso.class);
        verify(recursos).save(guardado.capture());
        assertFalse(guardado.getValue().getFechaExpiracionInvitacion().isBefore(antes.plusDays(7)));
        assertFalse(guardado.getValue().getFechaExpiracionInvitacion().isAfter(LocalDateTime.now().plusDays(7)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ANTIGUA", "VENCIDA", "OTRA_CUENTA"})
    void renovarAsignacionInvalidaElTokenAnteriorYConservaRabbit(String caso) throws Exception {
        String anterior = invitacionPendiente();
        RecursoDTO dto = edicion();
        if (caso.equals("ANTIGUA")) recurso.setFechaExpiracionInvitacion(null);
        if (caso.equals("VENCIDA")) recurso.setFechaExpiracionInvitacion(LocalDateTime.now().minusSeconds(1));
        if (caso.equals("OTRA_CUENTA")) dto.setEmailUsuario(OTHER_EMPLOYEE);
        LocalDateTime antes = LocalDateTime.now();
        mvc.perform(conToken(body(put("/api/recursos/1"), dto), OWNER)).andExpect(status().isOk());
        assertNotEquals(anterior, recurso.getTokenInvitacion());
        assertEquals(4, UUID.fromString(recurso.getTokenInvitacion()).version());
        assertFalse(recurso.getFechaExpiracionInvitacion().isBefore(antes.plusDays(7)));
        assertFalse(recurso.getFechaExpiracionInvitacion().isAfter(LocalDateTime.now().plusDays(7)));
        assertEquals(EstadoAsociacion.PENDIENTE, recurso.getEstadoAsociacion());
        verify(outbox).encolar(eq("notificaciones.exchange"), eq("invitacion.email.routing.key"),
                any(com.microservice.reservas.dto.event.InvitacionEmpleadoEvent.class));
        mvc.perform(body(post("/api/recursos/invitacion/aceptar"), Map.of("token", anterior))).andExpect(status().isNotFound());
    }

    @Test
    void guardarAsignacionPendienteVigenteNoRegeneraTokenNiCorreo() throws Exception {
        String anterior = invitacionPendiente();
        mvc.perform(conToken(body(put("/api/recursos/1"), edicion()), OWNER)).andExpect(status().isOk());
        assertEquals(anterior, recurso.getTokenInvitacion());
        verifyNoInteractions(outbox);
    }

    @Test
    void reservaValidaConservaContratoYClienteAutenticado() throws Exception {
        configurarJornadaParaReserva();
        mvc.perform(conToken(body(post("/api/turnos"), reservaDTO()), CLIENT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMADO"));
        verify(recursos).findByIdActivoParaActualizar(1L);
        verify(turnos).existeSolapamiento(eq(1L), any(), any(), eq(EstadoTurno.CONFIRMADO));
        verify(turnos).save(any());
    }

    @Test
    void reservaSinRecursoExplicitoSigueDerivandoloDelServicio() throws Exception {
        configurarJornadaParaReserva();
        var dto = new HashMap<>(reservaDTO());
        dto.remove("idRecurso");
        mvc.perform(conToken(body(post("/api/turnos"), dto), CLIENT)).andExpect(status().isOk());
    }

    @Test
    void reservaOcupadaDevuelve409ConMensaje() throws Exception {
        configurarJornadaParaReserva();
        when(turnos.existeSolapamiento(eq(1L), any(), any(), eq(EstadoTurno.CONFIRMADO))).thenReturn(true);
        mvc.perform(conToken(body(post("/api/turnos"), reservaDTO()), CLIENT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Elegí otro horario")));
        verify(turnos, never()).save(any());
        verifyNoInteractions(outbox);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PASADO", "SIN_SERVICIO", "SERVICIO_NEGATIVO", "RECURSO_NEGATIVO", "OTRO_RECURSO", "FUERA_JORNADA"})
    void reservaConDatosInvalidosDevuelve400(String caso) throws Exception {
        configurarJornadaParaReserva();
        var dto = new HashMap<>(reservaDTO());
        switch (caso) {
            case "PASADO" -> dto.put("fechaHoraInicio", LocalDateTime.now().minusDays(1).toString());
            case "SIN_SERVICIO" -> dto.remove("idServicio");
            case "SERVICIO_NEGATIVO" -> dto.put("idServicio", -1L);
            case "RECURSO_NEGATIVO" -> dto.put("idRecurso", -1L);
            case "OTRO_RECURSO" -> dto.put("idRecurso", 2L);
            case "FUERA_JORNADA" -> dto.put("fechaHoraInicio", LocalDateTime.now().plusDays(2).with(LocalTime.of(8, 0)).toString());
        }
        mvc.perform(conToken(body(post("/api/turnos"), dto), CLIENT)).andExpect(status().isBadRequest());
        verify(turnos, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECURSO", "SERVICIO"})
    void referenciasInexistentesDevuelven404(String caso) throws Exception {
        if (caso.equals("RECURSO")) when(recursos.findByIdActivoParaActualizar(1L)).thenReturn(Optional.empty());
        else when(servicios.findByIdAndActivoTrue(1L)).thenReturn(Optional.empty());
        mvc.perform(conToken(body(post("/api/turnos"), reservaDTO()), CLIENT)).andExpect(status().isNotFound());
        verify(turnos, never()).save(any());
    }

    @Test
    void crearTurnoExigeSesionYCuentaHabilitada() throws Exception {
        mvc.perform(body(post("/api/turnos"), reservaDTO())).andExpect(status().isUnauthorized());
        cuentas.get(CLIENT).setEstadoCuenta(EstadoCuenta.SUSPENDIDO);
        mvc.perform(conToken(body(post("/api/turnos"), reservaDTO()), CLIENT)).andExpect(status().isUnauthorized());
        verify(turnos, never()).save(any());
    }

    @Test
    void cancelarRepetidamenteYReconfirmarDevuelveConflicto() throws Exception {
        mvc.perform(conToken(delete("/api/turnos/1"), CLIENT)).andExpect(status().isOk());
        mvc.perform(conToken(delete("/api/turnos/1"), CLIENT)).andExpect(status().isConflict());
        mvc.perform(conToken(patch("/api/turnos/1/estado").param("nuevoEstado", "CONFIRMADO"), CLIENT))
                .andExpect(status().isConflict());
        verify(turnos, times(1)).save(any());
    }

    @Test
    void clienteAjenoNoCambiaEstadoNiCancela() throws Exception {
        mvc.perform(conToken(delete("/api/turnos/1"), OTHER_EMPLOYEE)).andExpect(status().isForbidden());
        mvc.perform(conToken(patch("/api/turnos/1/estado").param("nuevoEstado", "CANCELADO"), OTHER_EMPLOYEE))
                .andExpect(status().isForbidden());
        verify(turnos, never()).save(any());
    }

    private Map<String, Object> reservaDTO() {
        return Map.of("idServicio", 1L, "idRecurso", 1L,
                "fechaHoraInicio", LocalDateTime.now().plusDays(2).with(LocalTime.of(10, 0)).toString());
    }

    private void configurarJornadaParaReserva() {
        recurso.setHorarios(Arrays.stream(DayOfWeek.values()).map(dia -> {
            HorarioRecurso h = new HorarioRecurso();
            h.setDiaSemana(dia);
            h.setHoraInicio(LocalTime.of(9, 0));
            h.setHoraFin(LocalTime.of(18, 0));
            return h;
        }).toList());
    }

    private String invitacionPendiente() {
        recurso.setEstadoAsociacion(EstadoAsociacion.PENDIENTE);
        recurso.setTokenInvitacion(UUID.randomUUID().toString());
        recurso.setFechaExpiracionInvitacion(LocalDateTime.now().plusDays(7));
        return recurso.getTokenInvitacion();
    }

    private MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder request, String email) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(cuentas.get(email)));
    }

    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, Object dto) {
        return request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dto));
    }

    private MockHttpServletRequestBuilder reporte(Long negocioId, Long recursoId) {
        var request = get("/api/turnos/negocio/{id}/reportes", negocioId)
                .param("fechaInicio", START.toString()).param("fechaFin", START.plusYears(1).toString());
        return recursoId == null ? request : request.param("recursoId", recursoId.toString());
    }

    private RecursoDTO edicion() {
        RecursoDTO dto = new RecursoDTO();
        dto.setNombre("Recurso");
        dto.setDescripcion("Descripcion");
        dto.setIdNegocio(1L);
        dto.setEmailUsuario(EMPLOYEE);
        dto.setHorarios(Arrays.stream(DayOfWeek.values()).map(dia -> {
            HorarioSemanalDTO h = new HorarioSemanalDTO();
            h.setDiaSemana(dia);
            h.setActivo(true);
            h.setHoraInicio(LocalTime.of(9, 0));
            h.setHoraFin(LocalTime.of(18, 0));
            return h;
        }).toList());
        return dto;
    }

    private ServicioDTO servicioDTO() {
        ServicioDTO dto = new ServicioDTO();
        dto.setIdRecurso(1L);
        dto.setNombre("Servicio");
        dto.setDuracionMinutos(30);
        dto.setPrecio(100.0);
        return dto;
    }

    private Negocio negocio(Long id, Usuario owner) {
        Negocio n = new Negocio();
        n.setId(id);
        n.setOwner(owner);
        n.setNombre("Local " + id);
        n.setAlias("local-" + id);
        n.setDireccion("Direccion comercial");
        n.setTelefono("011-publico");
        n.setEmailContacto("contacto@local.example");
        n.setHorarios(Arrays.stream(DayOfWeek.values()).map(dia -> {
            HorarioNegocio h = new HorarioNegocio();
            h.setDiaSemana(dia);
            h.setAbierto(true);
            h.setHoraApertura(LocalTime.of(9, 0));
            h.setHoraCierre(LocalTime.of(18, 0));
            return h;
        }).toList());
        return n;
    }

    private Recurso recurso(Long id, Negocio n, Usuario empleado) {
        Recurso r = new Recurso();
        r.setId(id);
        r.setNombre("Recurso");
        r.setNegocio(n);
        r.setUsuario(empleado);
        r.setEstadoAsociacion(EstadoAsociacion.ACTIVO);
        r.setTokenInvitacion("token-interno-no-publicar");
        r.setHorarios(new ArrayList<>());
        return r;
    }
}
