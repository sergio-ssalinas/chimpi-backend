package com.lavabarber.api.security;

import com.lavabarber.api.repository.UsuarioInternoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioInternoDetailsService implements UserDetailsService {

    private final UsuarioInternoRepository usuarioInternoRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioInternoRepository.findByUsername(username)
                .map(UsuarioInternoDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario interno no encontrado: " + username));
    }
}
