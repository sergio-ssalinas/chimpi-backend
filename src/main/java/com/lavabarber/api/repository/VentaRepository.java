package com.lavabarber.api.repository;

import java.time.LocalDateTime;

import com.lavabarber.api.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface VentaRepository extends JpaRepository<Venta, UUID> {

    List<Venta> findByCajaIdOrderByFechaDesc(UUID cajaId);

    List<Venta> findByNegocioIdOrderByFechaDesc(UUID negocioId);

    @Query("""
           select coalesce(sum(v.total), 0) from Venta v
           where v.caja.id = :cajaId
             and v.metodoPago = com.lavabarber.api.entity.enums.MetodoPago.EFECTIVO
             and v.estado = com.lavabarber.api.entity.enums.EstadoVenta.COMPLETADA
           """)
    BigDecimal totalEfectivoDeCaja(@Param("cajaId") UUID cajaId);

    List<Venta> findByNegocioIdAndFechaGreaterThanEqualAndFechaLessThanOrderByFechaDesc(
            UUID negocioId, LocalDateTime desde, LocalDateTime hastaExclusivo);
}
