package com.lavabarber.api.repository;

import com.lavabarber.api.entity.EtapaServicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EtapaServicioRepository extends JpaRepository<EtapaServicio, UUID> {

    List<EtapaServicio> findByServicioIdOrderByOrdenAsc(UUID servicioId);
}
