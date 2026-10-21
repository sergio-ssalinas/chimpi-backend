package com.lavabarber.api.repository;

import com.lavabarber.api.entity.Rubro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RubroRepository extends JpaRepository<Rubro, UUID> {
}
