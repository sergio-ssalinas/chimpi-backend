package com.lavabarber.api.repository;

import com.lavabarber.api.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProveedorRepository extends JpaRepository<Proveedor, UUID> {
    List<Proveedor> findByNegocioId(UUID negocioId);
}
