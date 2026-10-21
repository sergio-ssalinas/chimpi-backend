package com.lavabarber.api.repository;

import com.lavabarber.api.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    List<Reserva> findByClienteId(UUID clienteId);

    List<Reserva> findByNegocioId(UUID negocioId);
}
