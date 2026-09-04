package com.microservice.reservas.entity;

import com.microservice.reservas.enums.EstadoCuenta;
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

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCuenta estadoCuenta = EstadoCuenta.PENDIENTE_VERIFICACION;

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("USUARIO_REGISTRADO"));
    }

    @Override
    @NonNull
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isEnabled() {
        return this.estadoCuenta == EstadoCuenta.ACTIVO;
    }
}