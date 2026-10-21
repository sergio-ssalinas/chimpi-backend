package com.lavabarber.api.security;

import com.lavabarber.api.entity.Cliente;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Adapta la entidad Cliente al contrato UserDetails que necesita Spring Security. */
@Getter
public class ClienteUserDetails implements UserDetails {

    private final UUID clienteId;
    private final String username;
    private final String passwordHash;
    private final boolean activo;

    public ClienteUserDetails(Cliente cliente) {
        this.activo = cliente.isActivo();
        this.clienteId = cliente.getId();
        this.username = cliente.getUsername();
        this.passwordHash = cliente.getPasswordHash();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
