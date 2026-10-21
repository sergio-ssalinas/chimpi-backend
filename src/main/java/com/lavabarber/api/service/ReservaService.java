package com.lavabarber.api.service;

import com.lavabarber.api.dto.*;
import com.lavabarber.api.entity.*;
import com.lavabarber.api.entity.enums.EstadoReserva;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Logica central de creacion de reservas. Soporta uno o varios items
 * en la misma reserva (ej: lavadero + corte en simultaneo con distintos
 * empleados), validando disponibilidad real antes de confirmar.
 */
@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final ReservaItemRepository reservaItemRepository;
    private final ClienteRepository clienteRepository;
    private final NegocioRepository negocioRepository;
    private final ServicioRepository servicioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final DisponibilidadRepository disponibilidadRepository;

    @Transactional
    public ReservaResponse crearReserva(UUID clienteId, ReservaRequest request) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + clienteId));
        Negocio negocio = negocioRepository.findById(request.negocioId())
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado: " + request.negocioId()));

        Reserva reserva = Reserva.builder()
                .cliente(cliente)
                .negocio(negocio)
                .fecha(request.fecha())
                .estado(EstadoReserva.PENDIENTE)
                .build();

        List<ReservaItem> items = new ArrayList<>();
        for (ReservaItemRequest itemReq : request.items()) {
            items.add(construirItemValidado(reserva, itemReq, request.fecha(), items));
        }
        reserva.setItems(items);

        Reserva guardada = reservaRepository.save(reserva);
        return toResponse(guardada);
    }

    private ReservaItem construirItemValidado(Reserva reserva, ReservaItemRequest itemReq,
                                                java.time.LocalDate fecha, List<ReservaItem> yaValidadosEnEstaReserva) {
        Servicio servicio = servicioRepository.findById(itemReq.servicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado: " + itemReq.servicioId()));
        Empleado empleado = empleadoRepository.findById(itemReq.empleadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + itemReq.empleadoId()));

        if (empleado.getServicios().stream().noneMatch(s -> s.getId().equals(servicio.getId()))) {
            throw new BusinessException(
                    "El empleado " + empleado.getNombre() + " no realiza el servicio " + servicio.getNombre());
        }

        LocalTime horaInicio = itemReq.horaInicio();
        LocalTime horaFin = horaInicio.plusMinutes(servicio.getDuracionMinutos());

        validarDentroDeDisponibilidad(empleado, fecha, horaInicio, horaFin);
        validarSinSolapamientoConBaseDeDatos(empleado, fecha, horaInicio, horaFin);
        validarSinSolapamientoConMismaReserva(empleado, horaInicio, horaFin, yaValidadosEnEstaReserva);

        return ReservaItem.builder()
                .reserva(reserva)
                .servicio(servicio)
                .empleado(empleado)
                .horaInicio(horaInicio)
                .horaFin(horaFin)
                .build();
    }

    private void validarDentroDeDisponibilidad(Empleado empleado, java.time.LocalDate fecha,
                                                LocalTime horaInicio, LocalTime horaFin) {
        boolean dentroDeAlgunaVentana = disponibilidadRepository
                .findByEmpleadoIdAndDiaSemana(empleado.getId(), fecha.getDayOfWeek())
                .stream()
                .anyMatch(v -> !horaInicio.isBefore(v.getHoraInicio()) && !horaFin.isAfter(v.getHoraFin()));

        if (!dentroDeAlgunaVentana) {
            throw new BusinessException(
                    "El empleado " + empleado.getNombre() + " no atiende en ese horario (" + horaInicio + " - " + horaFin + ")");
        }
    }

    private void validarSinSolapamientoConBaseDeDatos(Empleado empleado, java.time.LocalDate fecha,
                                                        LocalTime horaInicio, LocalTime horaFin) {
        List<ReservaItem> solapados = reservaItemRepository.findSolapados(empleado.getId(), fecha, horaInicio, horaFin);
        if (!solapados.isEmpty()) {
            throw new BusinessException(
                    "El empleado " + empleado.getNombre() + " ya tiene una reserva en ese horario");
        }
    }

    private void validarSinSolapamientoConMismaReserva(Empleado empleado, LocalTime horaInicio, LocalTime horaFin,
                                                          List<ReservaItem> yaValidados) {
        boolean choqueInterno = yaValidados.stream()
                .filter(i -> i.getEmpleado().getId().equals(empleado.getId()))
                .anyMatch(i -> horaInicio.isBefore(i.getHoraFin()) && horaFin.isAfter(i.getHoraInicio()));
        if (choqueInterno) {
            throw new BusinessException(
                    "Estas pidiendo dos servicios con " + empleado.getNombre() + " al mismo tiempo");
        }
    }

    @Transactional
    public ReservaResponse cancelarReserva(UUID clienteId, UUID reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + reservaId));
        verificarPertenece(reserva, clienteId);
        reserva.setEstado(EstadoReserva.CANCELADA);
        return toResponse(reservaRepository.save(reserva));
    }

    @Transactional(readOnly = true)
    public ReservaResponse obtenerReserva(UUID clienteId, UUID reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + reservaId));
        verificarPertenece(reserva, clienteId);
        return toResponse(reserva);
    }

    private void verificarPertenece(Reserva reserva, UUID clienteId) {
        if (!reserva.getCliente().getId().equals(clienteId)) {
            // Se informa como "no encontrada" en vez de "prohibida" para no
            // confirmarle a un tercero que el ID de reserva existe.
            throw new ResourceNotFoundException("Reserva no encontrada: " + reserva.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> reservasDeCliente(UUID clienteId) {
        return reservaRepository.findByClienteId(clienteId).stream().map(this::toResponse).toList();
    }

    private ReservaResponse toResponse(Reserva reserva) {
        List<ReservaItemResponse> items = reserva.getItems().stream()
                .map(i -> new ReservaItemResponse(
                        i.getId(),
                        i.getServicio().getId(),
                        i.getServicio().getNombre(),
                        i.getEmpleado().getId(),
                        i.getEmpleado().getNombre(),
                        i.getHoraInicio(),
                        i.getHoraFin()))
                .toList();

        return new ReservaResponse(
                reserva.getId(),
                reserva.getCliente().getId(),
                reserva.getNegocio().getId(),
                reserva.getFecha(),
                reserva.getEstado(),
                items);
    }
}
