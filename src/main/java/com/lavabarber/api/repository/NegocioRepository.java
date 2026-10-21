package com.lavabarber.api.repository;

import com.lavabarber.api.entity.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NegocioRepository extends JpaRepository<Negocio, UUID> {
}
