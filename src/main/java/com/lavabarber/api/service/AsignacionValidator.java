package com.lavabarber.api.service;

import com.lavabarber.api.entity.Disponibilidad;
import com.lavabarber.api.entity.Empleado;
import com.lavabarber.api.entity.ReservaItem;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.repository.DisponibilidadRepository;
import com.lavabarber.api.repository.ReservaItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Valida que un empleado pueda tomar un horario puntual: que este dentro
 * de su disponibilidad semanal, y que no choque con otra reserva suya.
 * La usan tanto la creacion de reservas (ReservaService) como la
 * reasignacion de turnos (ReservaOperativaService).
 */
@Component
@RequiredArgsConstructor
public class AsignacionValidator {

    private final DisponibilidadRepository disponibilidadRepository;
    private final ReservaItemRepository reservaItemRepository;

    /**
     * @param itemAExcluir si se esta reasignando un item existente, su propio ID
     *                     (para no chocar consigo mismo). Null si es una reserva nueva.
     */
    public void validar(Empleado empleado, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, UUID itemAExcluir) {
        boolean dentroDeDisponibilidad = disponibilidadRepository
                .findByEmpleadoIdAndDiaSemana(empleado.getId(), fecha.getDayOfWeek())
                .stream()
                .anyMatch(v -> !horaInicio.isBefore(v.getHoraInicio()) && !horaFin.isAfter(v.getHoraFin()));

        if (!dentroDeDisponibilidad) {
            throw new BusinessException(
                    "El empleado " + empleado.getNombre() + " no atiende en ese horario (" + horaInicio + " - " + horaFin + ")");
        }

        List<ReservaItem> solapados = reservaItemRepository.findSolapados(empleado.getId(), fecha, horaInicio, horaFin);
        boolean choque = solapados.stream().anyMatch(item -> itemAExcluir == null || !item.getId().equals(itemAExcluir));

        if (choque) {
            throw new BusinessException("El empleado " + empleado.getNombre() + " ya tiene una reserva en ese horario");
        }
    }
}
