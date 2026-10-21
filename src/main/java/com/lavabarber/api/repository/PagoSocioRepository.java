package com.lavabarber.api.repository;

import java.time.LocalDate;

import com.lavabarber.api.entity.PagoSocio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PagoSocioRepository extends JpaRepository<PagoSocio, UUID> {
    List<PagoSocio> findBySocioIdOrderByFechaDesc(UUID socioId);
    List<PagoSocio> findByNegocioIdOrderByFechaDesc(UUID negocioId);

    List<PagoSocio> findByNegocioIdAndFechaBetweenOrderByFechaDesc(UUID negocioId, LocalDate desde, LocalDate hasta);
}
