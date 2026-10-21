package com.lavabarber.api.repository;

import com.lavabarber.api.entity.Caja;
import com.lavabarber.api.entity.enums.EstadoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CajaRepository extends JpaRepository<Caja, UUID> {

    Optional<Caja> findByUsuarioInternoIdAndEstado(UUID usuarioInternoId, EstadoCaja estado);

    List<Caja> findByNegocioIdOrderByFechaAperturaDesc(UUID negocioId);

    List<Caja> findByNegocioIdAndEstadoOrderByFechaAperturaDesc(UUID negocioId, EstadoCaja estado);
}
