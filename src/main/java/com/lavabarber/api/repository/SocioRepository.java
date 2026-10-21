package com.lavabarber.api.repository;

import com.lavabarber.api.entity.Socio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface SocioRepository extends JpaRepository<Socio, UUID> {

    List<Socio> findByNegocioId(UUID negocioId);

    @Query("""
           select coalesce(sum(s.porcentajeParticipacion), 0) from Socio s
           where s.negocio.id = :negocioId and s.activo = true and s.id <> :excluirId
           """)
    BigDecimal sumaPorcentajesActivos(@Param("negocioId") UUID negocioId, @Param("excluirId") UUID excluirId);
}
