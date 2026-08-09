package com.microservice.reservas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String apellido;

    @Column(unique = true, nullable = false)
    private String email;

    private String telefono;

    // ¡Acá está el campo nuevo para guardar la contraseña encriptada!
    @Column(nullable = false)
    private String password;

    @Column(columnDefinition = "boolean default true")
    private Boolean activo = true;

    // --- MÉTODOS OBLIGATORIOS DE SPRING SECURITY (USERDETAILS) ---

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Le asignamos un permiso base a todos los usuarios
        return List.of(new SimpleGrantedAuthority("USUARIO_REGISTRADO"));
    }

    @Override
    @NonNull
    public String getUsername() {
        // Spring necesita saber qué usamos como identificador en el login
        return this.email;
    }

    @Override
    public boolean isEnabled() {
        // Conectamos el estado de Spring Security con tu campo "activo"
        return this.activo;
    }
}