package com.microservice.reservas.service;

import com.microservice.reservas.dto.request.HorarioSemanalDTO;
import com.microservice.reservas.dto.request.RecursoDTO;
import com.microservice.reservas.entity.*;
import com.microservice.reservas.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.microservice.reservas.service.OutboxService;

import java.time.*;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecursoHorarioTest {
    @Mock private RecursoRepository recursos;
    @Mock private UsuarioRepository usuarios;
    @Mock private NegocioRepository negocios;
    @Mock private ServicioRepository servicios;
    @Mock private TurnoRepository turnos;
    @Mock private OutboxService outbox;
    @Mock private NegocioService negocioService;
    @InjectMocks private RecursoService service;
    private Negocio negocio;

    @BeforeEach
    void preparar() {
        Usuario owner = new Usuario();
        owner.setEmail("owner@example.com");
        negocio = new Negocio();
        negocio.setOwner(owner);
        negocio.setHorarios(Arrays.stream(DayOfWeek.values()).map(d -> {
            HorarioNegocio h = new HorarioNegocio();
            h.setDiaSemana(d);
            h.setHoraApertura(LocalTime.of(22, 0));
            h.setHoraCierre(LocalTime.of(2, 0));
            return h;
        }).toList());
        when(negocios.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(negocio));
    }

    @Test
    void recursoNocturnoSeGuardaDentroDeLaJornadaNocturna() {
        when(recursos.save(any())).thenAnswer(c -> c.getArgument(0));
        assertDoesNotThrow(() -> service.crearRecurso(dto("22:30", "01:30"), "owner@example.com"));
        verifyNoInteractions(outbox);
    }

    @Test
    void comparacionDeHorasNoPermiteRecursoNocturnoEnNegocioDiurno() {
        negocio.getHorarios().forEach(h -> { h.setHoraApertura(LocalTime.of(9, 0)); h.setHoraCierre(LocalTime.of(18, 0)); });
        assertThrows(RuntimeException.class, () -> service.crearRecurso(dto("22:30", "01:30"), "owner@example.com"));
        verify(recursos, never()).save(any());
    }

    @Test
    void recursoDeMadrugadaPuedeEstarDentroDeJornadaDelNegocioAnterior() {
        negocio.getHorarios().forEach(h -> h.setAbierto(h.getDiaSemana() == DayOfWeek.SUNDAY));
        RecursoDTO dto = dto("00:30", "01:30");
        dto.getHorarios().forEach(h -> h.setActivo(h.getDiaSemana() == DayOfWeek.MONDAY));
        when(recursos.save(any())).thenAnswer(c -> c.getArgument(0));
        assertDoesNotThrow(() -> service.crearRecurso(dto, "owner@example.com"));
    }

    private RecursoDTO dto(String inicio, String fin) {
        RecursoDTO dto = new RecursoDTO();
        dto.setNombre("Recurso");
        dto.setIdNegocio(1L);
        dto.setHorarios(Arrays.stream(DayOfWeek.values()).map(d -> {
            HorarioSemanalDTO h = new HorarioSemanalDTO();
            h.setDiaSemana(d);
            h.setActivo(true);
            h.setHoraInicio(LocalTime.parse(inicio));
            h.setHoraFin(LocalTime.parse(fin));
            return h;
        }).toList());
        return dto;
    }
}
