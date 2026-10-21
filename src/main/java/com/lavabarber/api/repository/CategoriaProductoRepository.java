package com.lavabarber.api.repository;

import com.lavabarber.api.entity.CategoriaProducto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoriaProductoRepository extends JpaRepository<CategoriaProducto, UUID> {
    List<CategoriaProducto> findByNegocioId(UUID negocioId);
}
