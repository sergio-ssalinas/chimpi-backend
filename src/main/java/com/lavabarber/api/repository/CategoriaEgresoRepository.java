package com.lavabarber.api.repository;

import com.lavabarber.api.entity.CategoriaEgreso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoriaEgresoRepository extends JpaRepository<CategoriaEgreso, UUID> {
    List<CategoriaEgreso> findByNegocioId(UUID negocioId);
}
