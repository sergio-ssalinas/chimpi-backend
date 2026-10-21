package com.lavabarber.api.service;

import com.lavabarber.api.dto.HorarioDisponibleResponse;
import com.lavabarber.api.dto.HorarioComboComponenteResponse;
import com.lavabarber.api.dto.HorarioComboResponse;
import com.lavabarber.api.dto.EmpleadoResponse;
import com.lavabarber.api.entity.Disponibilidad;
import com.lavabarber.api.entity.Empleado;
import com.lavabarber.api.entity.ReservaItem;
import com.lavabarber.api.entity.Servicio;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.DisponibilidadRepository;
import com.lavabarber.api.repository.EmpleadoRepository;
import com.lavabarber.api.repository.ReservaItemRepository;
import com.lavabarber.api.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Calcula los horarios libres para un servicio dado, opcionalmente
 * filtrando por un empleado especifico. Este es el corazon de la
 * funcionalidad "ver turnos disponibles" que consume la app movil.
 */
@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    /** Cada cuantos minutos se ofrece un slot candidato (granularidad de la agenda). */
    private static final int GRANULARIDAD_MIN = 15;

    private final ServicioRepository servicioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final ReservaItemRepository reservaItemRepository;

    public List<HorarioDisponibleResponse> horariosDisponibles(UUID servicioId, LocalDate fecha, UUID empleadoIdFiltro) {
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado: " + servicioId));

        List<Empleado> candidatos = empleadoIdFiltro != null
                ? List.of(empleadoRepository.findById(empleadoIdFiltro)
                        .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + empleadoIdFiltro)))
                : empleadoRepository.findDisponiblesParaServicio(servicioId);

        List<HorarioDisponibleResponse> resultado = new ArrayList<>();

        for (Empleado empleado : candidatos) {
            if (empleadoIdFiltro != null && empleado.getServicios().stream().noneMatch(s -> s.getId().equals(servicioId))) {
                continue; // el empleado filtrado no realiza este servicio
            }
            resultado.addAll(slotsLibresDeEmpleado(empleado, servicio, fecha));
        }
        return resultado;
    }

    private List<HorarioDisponibleResponse> slotsLibresDeEmpleado(Empleado empleado, Servicio servicio, LocalDate fecha) {
        List<HorarioDisponibleResponse> libres = new ArrayList<>();
        int duracion = servicio.getDuracionMinutos();

        List<Disponibilidad> ventanas = disponibilidadRepository
                .findByEmpleadoIdAndDiaSemana(empleado.getId(), fecha.getDayOfWeek());

        for (Disponibilidad ventana : ventanas) {
            LocalTime cursor = ventana.getHoraInicio();
            while (!cursor.plusMinutes(duracion).isAfter(ventana.getHoraFin())) {
                LocalTime fin = cursor.plusMinutes(duracion);
                List<ReservaItem> ocupados = reservaItemRepository
                        .findSolapados(empleado.getId(), fecha, cursor, fin);
                if (ocupados.isEmpty()) {
                    libres.add(new HorarioDisponibleResponse(empleado.getId(), empleado.getNombre(), cursor, fin));
                }
                cursor = cursor.plusMinutes(GRANULARIDAD_MIN);
            }
        }
        return libres;
    }

    /**
     * Calcula, para un servicio Mixto (combo), las horas de inicio en las que
     * TODOS sus componentes tienen simultaneamente al menos un empleado libre.
     * Es obligatorio que el cliente elija horario para cada componente, y
     * todos arrancan a la misma hora (reserva en paralelo).
     */
    public List<HorarioComboResponse> horariosComboDisponibles(UUID comboServicioId, LocalDate fecha) {
        Servicio combo = servicioRepository.findById(comboServicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado: " + comboServicioId));

        if (!combo.esCombo()) {
            throw new BusinessException(combo.getNombre() + " no es un servicio combinado");
        }

        // horaInicio -> componenteServicioId -> empleados libres a esa hora
        Map<LocalTime, Map<UUID, List<Empleado>>> disponibilidadPorHora = new TreeMap<>();

        for (Servicio componente : combo.getComponentes()) {
            List<Empleado> empleados = empleadoRepository.findDisponiblesParaServicio(componente.getId());
            for (Empleado empleado : empleados) {
                List<Disponibilidad> ventanas = disponibilidadRepository
                        .findByEmpleadoIdAndDiaSemana(empleado.getId(), fecha.getDayOfWeek());

                for (Disponibilidad ventana : ventanas) {
                    LocalTime cursor = ventana.getHoraInicio();
                    int duracion = componente.getDuracionMinutos();
                    while (!cursor.plusMinutes(duracion).isAfter(ventana.getHoraFin())) {
                        LocalTime fin = cursor.plusMinutes(duracion);
                        boolean libre = reservaItemRepository.findSolapados(empleado.getId(), fecha, cursor, fin).isEmpty();
                        if (libre) {
                            disponibilidadPorHora
                                    .computeIfAbsent(cursor, k -> new HashMap<>())
                                    .computeIfAbsent(componente.getId(), k -> new ArrayList<>())
                                    .add(empleado);
                        }
                        cursor = cursor.plusMinutes(GRANULARIDAD_MIN);
                    }
                }
            }
        }

        List<HorarioComboResponse> resultado = new ArrayList<>();
        for (Map.Entry<LocalTime, Map<UUID, List<Empleado>>> entry : disponibilidadPorHora.entrySet()) {
            Map<UUID, List<Empleado>> porComponente = entry.getValue();

            boolean todosLosComponentesTienenHueco = combo.getComponentes().stream()
                    .allMatch(c -> porComponente.getOrDefault(c.getId(), List.of()).size() > 0);
            if (!todosLosComponentesTienenHueco) continue;

            List<HorarioComboComponenteResponse> componentesResp = combo.getComponentes().stream()
                    .map(c -> new HorarioComboComponenteResponse(
                            c.getId(),
                            c.getNombre(),
                            c.getDuracionMinutos(),
                            porComponente.get(c.getId()).stream()
                                    .map(e -> new EmpleadoResponse(e.getId(), e.getNombre(), e.getFotoUrl()))
                                    .toList()))
                    .toList();

            resultado.add(new HorarioComboResponse(entry.getKey(), componentesResp));
        }
        return resultado;
    }
}
