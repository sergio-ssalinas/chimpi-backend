package com.lavabarber.api.repository;

import com.lavabarber.api.entity.ReservaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ReservaItemRepository extends JpaRepository<ReservaItem, UUID> {

    /**
     * Trae los items ya reservados de un empleado en una fecha dada,
     * que se solapan con el rango [horaInicio, horaFin) propuesto.
     * Se excluyen reservas CANCELADA para no bloquear horarios liberados.
     */
    @Query("""
           select ri from ReservaItem ri
           where ri.empleado.id = :empleadoId
             and ri.reserva.fecha = :fecha
             and ri.reserva.estado <> com.lavabarber.api.entity.enums.EstadoReserva.CANCELADA
             and ri.horaInicio < :horaFin
             and ri.horaFin > :horaInicio
           """)
    List<ReservaItem> findSolapados(@Param("empleadoId") UUID empleadoId,
                                     @Param("fecha") LocalDate fecha,
                                     @Param("horaInicio") LocalTime horaInicio,
                                     @Param("horaFin") LocalTime horaFin);

    List<ReservaItem> findByEmpleadoIdAndReserva_Fecha(UUID empleadoId, LocalDate fecha);

    /**
     * Items de un negocio para una fecha dada (vista operativa del dia),
     * ordenados por horario. Se excluyen las reservas canceladas.
     */
    @Query("""
           select ri from ReservaItem ri
           where ri.reserva.negocio.id = :negocioId
             and ri.reserva.fecha = :fecha
             and ri.reserva.estado <> com.lavabarber.api.entity.enums.EstadoReserva.CANCELADA
           order by ri.horaInicio asc
           """)
    List<ReservaItem> findDelDia(@Param("negocioId") UUID negocioId, @Param("fecha") LocalDate fecha);
}
