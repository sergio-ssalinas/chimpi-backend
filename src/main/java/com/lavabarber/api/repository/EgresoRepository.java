package com.lavabarber.api.repository;

import java.time.LocalDate;

import com.lavabarber.api.entity.Egreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface EgresoRepository extends JpaRepository<Egreso, UUID> {

    List<Egreso> findByNegocioIdOrderByFechaDesc(UUID negocioId);

    @Query("""
           select coalesce(sum(e.monto), 0) from Egreso e
           where e.caja.id = :cajaId
             and e.metodoPago = com.lavabarber.api.entity.enums.MetodoPago.EFECTIVO
             and e.estado = com.lavabarber.api.entity.enums.EstadoEgreso.ACTIVO
           """)
    BigDecimal totalEfectivoDeCaja(@Param("cajaId") UUID cajaId);

    List<Egreso> findByNegocioIdAndFechaBetweenOrderByFechaDesc(UUID negocioId, LocalDate desde, LocalDate hasta);
}
