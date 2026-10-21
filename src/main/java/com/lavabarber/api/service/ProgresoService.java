package com.lavabarber.api.service;

import com.lavabarber.api.dto.ProgresoReservaItemResponse;
import com.lavabarber.api.entity.EtapaServicio;
import com.lavabarber.api.entity.Reserva;
import com.lavabarber.api.entity.ReservaItem;
import com.lavabarber.api.entity.enums.EstadoReserva;
import com.lavabarber.api.entity.enums.EstadoReservaItem;
import com.lavabarber.api.repository.EtapaServicioRepository;
import com.lavabarber.api.repository.ReservaRepository;
import com.lavabarber.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Calcula el progreso de cada ReservaItem. Si el empleado ya marco algo a
 * mano (confirmar presencia, avanzar etapa, finalizar, marcar ausente —
 * ver ReservaOperativaService), ese dato manda. Mientras el item siga en
 * PENDIENTE (nadie toco nada), se simula el avance por tiempo transcurrido
 * dentro de la franja horaria reservada, como fallback razonable.
 */
@Service
@RequiredArgsConstructor
public class ProgresoService {

    private final ReservaRepository reservaRepository;
    private final EtapaServicioRepository etapaServicioRepository;

    /** Progreso de todos los items de una reserva puntual (valida que sea del cliente). */
    public List<ProgresoReservaItemResponse> progresoDeReserva(UUID clienteId, UUID reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + reservaId));
        if (!reserva.getCliente().getId().equals(clienteId)) {
            throw new ResourceNotFoundException("Reserva no encontrada: " + reservaId);
        }
        return reserva.getItems().stream().map(this::calcularProgreso).toList();
    }

    /**
     * Progreso de todos los items "activos hoy" del cliente (reservas de hoy,
     * no canceladas). Pensado para una pantalla tipo "Mi progreso ahora".
     */
    public List<ProgresoReservaItemResponse> progresoActivoDeCliente(UUID clienteId) {
        LocalDate hoy = LocalDate.now();
        return reservaRepository.findByClienteId(clienteId).stream()
                .filter(r -> r.getFecha().isEqual(hoy))
                .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                .flatMap(r -> r.getItems().stream())
                .map(this::calcularProgreso)
                .toList();
    }

    private ProgresoReservaItemResponse calcularProgreso(ReservaItem item) {
        if (item.getReserva().getEstado() != EstadoReserva.CANCELADA && item.getEstado() != EstadoReservaItem.PENDIENTE) {
            return calcularProgresoManual(item);
        }
        return calcularProgresoPorTiempo(item);
    }

    /**
     * El empleado ya toco algo (confirmo presencia, avanzo etapa, finalizo,
     * o marco ausente): ese dato manda por sobre la simulacion por tiempo.
     */
    private ProgresoReservaItemResponse calcularProgresoManual(ReservaItem item) {
        String estado;
        int porcentaje;
        String etapaActual;

        switch (item.getEstado()) {
            case CONFIRMADO -> {
                estado = "EN_PROGRESO";
                porcentaje = 5;
                etapaActual = "Cliente confirmado";
            }
            case EN_PROGRESO -> {
                estado = "EN_PROGRESO";
                if (item.getEtapaActual() != null) {
                    etapaActual = item.getEtapaActual().getNombre();
                    porcentaje = item.getEtapaActual().getPorcentajeHasta();
                } else {
                    etapaActual = "En progreso";
                    porcentaje = 50;
                }
            }
            case FINALIZADO -> {
                estado = "FINALIZADO";
                porcentaje = 100;
                etapaActual = "Finalizado";
            }
            case NO_SHOW -> {
                estado = "CANCELADO";
                porcentaje = 0;
                etapaActual = "No se presento";
            }
            default -> {
                estado = "NO_INICIADO";
                porcentaje = 0;
                etapaActual = "En espera";
            }
        }

        return new ProgresoReservaItemResponse(
                item.getId(),
                item.getReserva().getId(),
                item.getServicio().getNombre(),
                item.getEmpleado().getNombre(),
                item.getHoraInicio(),
                item.getHoraFin(),
                estado,
                porcentaje,
                etapaActual);
    }

    /** Nadie toco nada todavia: se simula el avance por tiempo transcurrido, como antes. */
    private ProgresoReservaItemResponse calcularProgresoPorTiempo(ReservaItem item) {
        LocalDate fecha = item.getReserva().getFecha();
        LocalDateTime inicio = LocalDateTime.of(fecha, item.getHoraInicio());
        LocalDateTime fin = LocalDateTime.of(fecha, item.getHoraFin());
        LocalDateTime ahora = LocalDateTime.now();

        String estado;
        int porcentaje;

        if (item.getReserva().getEstado() == EstadoReserva.CANCELADA) {
            estado = "CANCELADO";
            porcentaje = 0;
        } else if (ahora.isBefore(inicio)) {
            estado = "NO_INICIADO";
            porcentaje = 0;
        } else if (!ahora.isBefore(fin)) {
            estado = "FINALIZADO";
            porcentaje = 100;
        } else {
            estado = "EN_PROGRESO";
            long totalSegundos = java.time.Duration.between(inicio, fin).getSeconds();
            long transcurridos = java.time.Duration.between(inicio, ahora).getSeconds();
            porcentaje = totalSegundos <= 0 ? 100 : (int) Math.round((transcurridos * 100.0) / totalSegundos);
            porcentaje = Math.max(0, Math.min(100, porcentaje));
        }

        String etapaActual = determinarEtapa(item.getServicio().getId(), porcentaje, estado);

        return new ProgresoReservaItemResponse(
                item.getId(),
                item.getReserva().getId(),
                item.getServicio().getNombre(),
                item.getEmpleado().getNombre(),
                item.getHoraInicio(),
                item.getHoraFin(),
                estado,
                porcentaje,
                etapaActual);
    }

    private String determinarEtapa(UUID servicioId, int porcentaje, String estado) {
        if (estado.equals("NO_INICIADO")) return "En espera";
        if (estado.equals("CANCELADO")) return "Cancelado";

        List<EtapaServicio> etapas = etapaServicioRepository.findByServicioIdOrderByOrdenAsc(servicioId);
        if (etapas.isEmpty()) {
            return estado.equals("FINALIZADO") ? "Finalizado" : "En progreso";
        }
        for (EtapaServicio etapa : etapas) {
            if (porcentaje <= etapa.getPorcentajeHasta()) {
                return etapa.getNombre();
            }
        }
        return etapas.get(etapas.size() - 1).getNombre();
    }
}
