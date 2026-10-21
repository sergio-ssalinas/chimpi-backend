package com.lavabarber.api.repository;

import com.lavabarber.api.entity.UsuarioInterno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioInternoRepository extends JpaRepository<UsuarioInterno, UUID> {

    Optional<UsuarioInterno> findByUsername(String username);

    boolean existsByUsername(String username);
}
