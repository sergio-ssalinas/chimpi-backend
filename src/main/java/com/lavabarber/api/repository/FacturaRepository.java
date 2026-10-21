package com.lavabarber.api.repository;

import com.lavabarber.api.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacturaRepository extends JpaRepository<Factura, UUID> {
    Optional<Factura> findByVentaId(UUID ventaId);
    List<Factura> findByNegocioIdOrderByFechaEmisionDesc(UUID negocioId);
}
