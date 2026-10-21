package com.lavabarber.api.repository;

import com.lavabarber.api.entity.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromocionRepository extends JpaRepository<Promocion, UUID> {
    List<Promocion> findByNegocioId(UUID negocioId);
}
