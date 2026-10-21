package com.lavabarber.api.repository;

import com.lavabarber.api.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServicioRepository extends JpaRepository<Servicio, UUID> {

    List<Servicio> findByNegocioId(UUID negocioId);

    List<Servicio> findByNegocioIdAndRubroId(UUID negocioId, UUID rubroId);
}
