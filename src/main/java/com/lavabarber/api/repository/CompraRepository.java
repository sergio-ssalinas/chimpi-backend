package com.lavabarber.api.repository;

import java.time.LocalDate;

import com.lavabarber.api.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CompraRepository extends JpaRepository<Compra, UUID> {
    List<Compra> findByNegocioIdOrderByFechaDesc(UUID negocioId);

    List<Compra> findByNegocioIdAndFechaBetweenOrderByFechaDesc(UUID negocioId, LocalDate desde, LocalDate hasta);
}
