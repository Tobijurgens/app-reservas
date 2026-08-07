package com.microservice.reservas.controller;

import com.microservice.reservas.entity.Turno;
import com.microservice.reservas.entity.Usuario;
import com.microservice.reservas.repository.TurnoRepository;
import com.microservice.reservas.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @PostMapping
    public ResponseEntity<Object> crearUsuario(@RequestBody Usuario usuario) {
        // 1. Buscamos el email SIN importar si está activo o borrado
        // (Asegurate de tener el metodo findByEmail clásico en el Repository)
        Optional<Usuario> usuarioExistente = usuarioRepository.findByEmail(usuario.getEmail());

        if (usuarioExistente.isPresent()) {
            Usuario usrBD = usuarioExistente.get();

            if (usrBD.getActivo()) {
                // 1A. Existe y está activo -> Frenamos todo
                return ResponseEntity.badRequest().body("Error: Ya existe un usuario registrado con el email " + usuario.getEmail());
            } else {
                // 1B. Existe pero estaba borrado -> LO REACTIVAMOS
                usrBD.setActivo(true);
                usrBD.setNombre(usuario.getNombre());
                usrBD.setApellido(usuario.getApellido());
                usrBD.setTelefono(usuario.getTelefono());

                Usuario usuarioReactivado = usuarioRepository.save(usrBD);
                return ResponseEntity.ok(usuarioReactivado);
            }
        }

        // 2. Si no existía para nada, lo creamos de cero
        usuario.setActivo(true); // Nos aseguramos que nazca activo
        Usuario nuevoUsuario = usuarioRepository.save(usuario);
        return ResponseEntity.ok(nuevoUsuario);
    }

    @GetMapping
    public ResponseEntity<List<Usuario>> obtenerTodos() {
        return ResponseEntity.ok(usuarioRepository.findByActivoTrue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> obtenerPorId(@PathVariable Long id) {
        return usuarioRepository.findByIdAndActivoTrue(id)
                .map(usuario -> ResponseEntity.ok((Object) usuario))
                .orElseGet(() -> ResponseEntity.badRequest().body("Error: Usuario no encontrado o inactivo."));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Object> obtenerPorEmail(@PathVariable String email) {
        return usuarioRepository.findByEmailAndActivoTrue(email)
                .map(usuario -> ResponseEntity.ok((Object) usuario))
                .orElseGet(() -> ResponseEntity.badRequest().body("Error: Usuario no encontrado o inactivo."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminarUsuario(@PathVariable Long id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) return ResponseEntity.badRequest().body("Error: El usuario no existe.");

        Usuario usuario = usuarioOpt.get();
        if (!usuario.getActivo()) return ResponseEntity.badRequest().body("Error: Ya estaba eliminado.");

        // 1. Dar de baja al usuario
        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        // 2. Cancelar turnos futuros donde este usuario era el CLIENTE
        List<String> estadosACancelar = Arrays.asList("PENDIENTE", "CONFIRMADO");
        List<Turno> turnosAfectados = turnoRepository.findByClienteIdAndEstadoIn(id, estadosACancelar);

        for (Turno turno : turnosAfectados) {
            turno.setEstado("CANCELADO");
            turnoRepository.save(turno);
        }

        return ResponseEntity.ok("Usuario eliminado lógicamente. Se cancelaron " + turnosAfectados.size() + " turnos pendientes.");
    }
}