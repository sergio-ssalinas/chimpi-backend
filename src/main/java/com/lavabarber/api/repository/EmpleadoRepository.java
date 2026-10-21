package com.lavabarber.api.repository;

import com.lavabarber.api.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EmpleadoRepository extends JpaRepository<Empleado, UUID> {

    List<Empleado> findByNegocioId(UUID negocioId);

    @Query("""
           select e from Empleado e
           join e.servicios s
           where s.id = :servicioId and e.activo = true
           """)
    List<Empleado> findDisponiblesParaServicio(@Param("servicioId") UUID servicioId);
}
