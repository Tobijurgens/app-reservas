package com.microservice.reservas.controller;

import com.microservice.reservas.dto.request.RecursoDTO;
import com.microservice.reservas.dto.request.InvitacionDecisionDTO;
import com.microservice.reservas.dto.response.InvitacionResponseDTO;
import com.microservice.reservas.dto.response.RecursoResponseDTO;
import com.microservice.reservas.dto.response.RecursoPublicoDTO;
import com.microservice.reservas.service.RecursoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recursos")
@RequiredArgsConstructor
public class RecursoController {

    private final RecursoService recursoService;

    @GetMapping({"/invitacion", "/invitacion/aceptar", "/invitacion/rechazar"})
    public ResponseEntity<InvitacionResponseDTO> obtenerInvitacion(@RequestParam("token") String token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recursoService.obtenerInvitacion(token));
    }

    @PostMapping("/invitacion/aceptar")
    public ResponseEntity<String> aceptarInvitacion(@Valid @RequestBody InvitacionDecisionDTO decision) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recursoService.aceptarInvitacion(decision.token()));
    }

    @PostMapping("/invitacion/rechazar")
    public ResponseEntity<String> rechazarInvitacion(@Valid @RequestBody InvitacionDecisionDTO decision) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recursoService.rechazarInvitacion(decision.token()));
    }

    @PostMapping
    public ResponseEntity<RecursoResponseDTO> crearRecurso(@Valid @RequestBody RecursoDTO recursoDto, Principal principal) {
        String emailLogueado = principal.getName();
        return ResponseEntity.ok(new RecursoResponseDTO(recursoService.crearRecurso(recursoDto, emailLogueado), emailLogueado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecursoResponseDTO> actualizarRecurso(
            @PathVariable Long id,
            @Valid @RequestBody RecursoDTO recursoDto,
            Principal principal) {
        String emailLogueado = principal.getName();
        return ResponseEntity.ok(new RecursoResponseDTO(recursoService.actualizarRecurso(id, recursoDto, emailLogueado), emailLogueado));
    }

    @GetMapping
    public ResponseEntity<List<RecursoPublicoDTO>> obtenerTodos() {
        List<RecursoPublicoDTO> lista = recursoService.obtenerTodos().stream()
                .map(RecursoPublicoDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecursoResponseDTO> obtenerPorId(@PathVariable Long id, Principal principal) {
        return recursoService.obtenerPorId(id, principal.getName())
                .map(recurso -> ResponseEntity.ok(new RecursoResponseDTO(recurso, principal.getName())))
                .orElseThrow(() -> new RuntimeException("Error: Recurso no encontrado o inactivo."));
    }

    @GetMapping("/negocio/{negocioId}")
    public ResponseEntity<List<RecursoPublicoDTO>> obtenerPorNegocio(@PathVariable Long negocioId) {
        List<RecursoPublicoDTO> lista = recursoService.obtenerPorNegocio(negocioId).stream()
                .map(RecursoPublicoDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/negocio/{negocioId}/todos")
    public ResponseEntity<List<RecursoResponseDTO>> obtenerTodosPorNegocio(@PathVariable Long negocioId, Principal principal) {
        List<RecursoResponseDTO> lista = recursoService.obtenerTodosPorNegocio(negocioId, principal.getName()).stream()
                .map(recurso -> new RecursoResponseDTO(recurso, principal.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/mis-asignaciones")
    public ResponseEntity<List<RecursoResponseDTO>> obtenerMisAsignaciones(Principal principal) {
        List<RecursoResponseDTO> lista = recursoService.obtenerMisAsignaciones(principal.getName())
                .stream()
                .map(recurso -> new RecursoResponseDTO(recurso, principal.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarRecurso(@PathVariable Long id, Principal principal) {
        String emailLogueado = principal.getName();
        return ResponseEntity.ok(recursoService.eliminarRecurso(id, emailLogueado));
    }
}
