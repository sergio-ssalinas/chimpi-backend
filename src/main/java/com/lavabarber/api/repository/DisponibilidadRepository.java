package com.lavabarber.api.repository;

import com.lavabarber.api.entity.Disponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, UUID> {

    List<Disponibilidad> findByEmpleadoIdAndDiaSemana(UUID empleadoId, DayOfWeek diaSemana);

    List<Disponibilidad> findByEmpleadoIdOrderByDiaSemanaAscHoraInicioAsc(UUID empleadoId);
}
