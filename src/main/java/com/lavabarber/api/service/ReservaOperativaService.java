package com.lavabarber.api.service;

import com.lavabarber.api.dto.ReservaRequest;
import com.lavabarber.api.dto.ReservaResponse;
import com.lavabarber.api.dto.backoffice.ReservaBackofficeRequest;
import com.lavabarber.api.dto.backoffice.EtapaServicioResponse;
import com.lavabarber.api.dto.backoffice.ReservaItemOperativoResponse;
import com.lavabarber.api.entity.Empleado;
import com.lavabarber.api.entity.EtapaServicio;
import com.lavabarber.api.entity.Reserva;
import com.lavabarber.api.entity.ReservaItem;
import com.lavabarber.api.entity.enums.EstadoReserva;
import com.lavabarber.api.entity.enums.EstadoReservaItem;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.EmpleadoRepository;
import com.lavabarber.api.repository.EtapaServicioRepository;
import com.lavabarber.api.repository.ReservaItemRepository;
import com.lavabarber.api.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Operacion del dia a dia para empleados/admin: confirmar presencia,
 * avanzar etapas a mano, finalizar, marcar ausente, y reasignar un turno
 * (empleado y/u horario) cuando hay un atraso o una falta.
 *
 * Un ADMIN puede operar cualquier turno. Un EMPLEADO solo los suyos
 * (los que tiene asignados a si mismo como Empleado).
 */
@Service
@RequiredArgsConstructor
public class ReservaOperativaService {

    private final ReservaItemRepository reservaItemRepository;
    private final ReservaRepository reservaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final EtapaServicioRepository etapaServicioRepository;
    private final AsignacionValidator asignacionValidator;
    private final ReservaService reservaService;

    @Transactional(readOnly = true)
    public List<ReservaItemOperativoResponse> listarDelDia(UUID negocioId, LocalDate fecha, UUID empleadoIdFiltro) {
        return reservaItemRepository.findDelDia(negocioId, fecha).stream()
                .filter(i -> empleadoIdFiltro == null || i.getEmpleado().getId().equals(empleadoIdFiltro))
                .map(this::toOperativoResponse)
                .toList();
    }

    /**
     * Agenda un turno para un cliente desde el backoffice, con las mismas validaciones que la app
     * (disponibilidad del empleado y choques). ADMIN puede agendar con cualquier empleado; un EMPLEADO
     * solo turnos a su propio nombre, igual que el resto de su operacion.
     */
    @Transactional
    public ReservaResponse crearParaCliente(ReservaBackofficeRequest request, UUID empleadoSolicitanteId, boolean esAdmin) {
        if (!esAdmin) {
            boolean todosSuyos = empleadoSolicitanteId != null
                    && request.items().stream().allMatch(i -> empleadoSolicitanteId.equals(i.empleadoId()));
            if (!todosSuyos) {
                throw new AccessDeniedException("Solo podes agendar turnos a tu nombre");
            }
        }
        return reservaService.crearReserva(request.clienteId(),
                new ReservaRequest(request.negocioId(), request.fecha(), request.items()));
    }

    @Transactional
    public ReservaItemOperativoResponse confirmar(UUID itemId, UUID empleadoSolicitanteId, boolean esAdmin) {
        ReservaItem item = obtenerConPermiso(itemId, empleadoSolicitanteId, esAdmin);
        if (item.getEstado() != EstadoReservaItem.PENDIENTE) {
            throw new BusinessException("Este turno ya fue confirmado o avanzado");
        }
        item.setEstado(EstadoReservaItem.CONFIRMADO);
        item.setHoraInicioReal(LocalDateTime.now());
        return toOperativoResponse(reservaItemRepository.save(item));
    }

    @Transactional
    public ReservaItemOperativoResponse avanzarEtapa(UUID itemId, UUID etapaId, UUID empleadoSolicitanteId, boolean esAdmin) {
        ReservaItem item = obtenerConPermiso(itemId, empleadoSolicitanteId, esAdmin);
        if (item.getEstado() == EstadoReservaItem.FINALIZADO || item.getEstado() == EstadoReservaItem.NO_SHOW) {
            throw new BusinessException("Este turno ya esta cerrado");
        }

        EtapaServicio etapa = etapaServicioRepository.findById(etapaId)
                .orElseThrow(() -> new ResourceNotFoundException("Etapa no encontrada: " + etapaId));
        if (!etapa.getServicio().getId().equals(item.getServicio().getId())) {
            throw new BusinessException("Esa etapa no pertenece a este servicio");
        }

        item.setEtapaActual(etapa);
        item.setEstado(EstadoReservaItem.EN_PROGRESO);
        if (item.getHoraInicioReal() == null) {
            item.setHoraInicioReal(LocalDateTime.now());
        }
        return toOperativoResponse(reservaItemRepository.save(item));
    }

    @Transactional
    public ReservaItemOperativoResponse finalizar(UUID itemId, UUID empleadoSolicitanteId, boolean esAdmin) {
        ReservaItem item = obtenerConPermiso(itemId, empleadoSolicitanteId, esAdmin);
        if (item.getEstado() == EstadoReservaItem.NO_SHOW) {
            throw new BusinessException("Este turno esta marcado como ausente");
        }
        item.setEstado(EstadoReservaItem.FINALIZADO);
        item.setHoraFinReal(LocalDateTime.now());
        item = reservaItemRepository.save(item);
        cerrarReservaSiCorresponde(item.getReserva());
        return toOperativoResponse(item);
    }

    @Transactional
    public ReservaItemOperativoResponse marcarAusente(UUID itemId, UUID empleadoSolicitanteId, boolean esAdmin) {
        ReservaItem item = obtenerConPermiso(itemId, empleadoSolicitanteId, esAdmin);
        if (item.getEstado() == EstadoReservaItem.FINALIZADO) {
            throw new BusinessException("Este turno ya fue finalizado");
        }
        item.setEstado(EstadoReservaItem.NO_SHOW);
        item = reservaItemRepository.save(item);
        cerrarReservaSiCorresponde(item.getReserva());
        return toOperativoResponse(item);
    }

    /**
     * Cambia el empleado y/o el horario de un turno ya existente (atraso o
     * falta del empleado original). Revalida disponibilidad y choques igual
     * que al crear una reserva nueva, y resetea el progreso manual del item
     * (arranca de cero con el nuevo empleado/horario).
     */
    @Transactional
    public ReservaItemOperativoResponse reasignar(UUID itemId, UUID nuevoEmpleadoId, LocalTime nuevaHoraInicio,
                                                    UUID empleadoSolicitanteId, boolean esAdmin) {
        ReservaItem item = obtenerConPermiso(itemId, empleadoSolicitanteId, esAdmin);
        if (item.getEstado() == EstadoReservaItem.FINALIZADO) {
            throw new BusinessException("No se puede reasignar un turno ya finalizado");
        }

        Empleado nuevoEmpleado = empleadoRepository.findById(nuevoEmpleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + nuevoEmpleadoId));

        if (nuevoEmpleado.getServicios().stream().noneMatch(s -> s.getId().equals(item.getServicio().getId()))) {
            throw new BusinessException(nuevoEmpleado.getNombre() + " no realiza " + item.getServicio().getNombre());
        }

        LocalTime nuevaHoraFin = nuevaHoraInicio.plusMinutes(item.getServicio().getDuracionMinutos());
        asignacionValidator.validar(nuevoEmpleado, item.getReserva().getFecha(), nuevaHoraInicio, nuevaHoraFin, item.getId());

        item.setEmpleado(nuevoEmpleado);
        item.setHoraInicio(nuevaHoraInicio);
        item.setHoraFin(nuevaHoraFin);
        // Arranca de cero con el nuevo empleado/horario.
        item.setEstado(EstadoReservaItem.PENDIENTE);
        item.setEtapaActual(null);
        item.setHoraInicioReal(null);
        item.setHoraFinReal(null);

        return toOperativoResponse(reservaItemRepository.save(item));
    }

    private ReservaItem obtenerConPermiso(UUID itemId, UUID empleadoSolicitanteId, boolean esAdmin) {
        ReservaItem item = reservaItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado: " + itemId));

        boolean esElMismoEmpleado = empleadoSolicitanteId != null && item.getEmpleado().getId().equals(empleadoSolicitanteId);
        if (!esAdmin && !esElMismoEmpleado) {
            throw new AccessDeniedException("No tenes permiso sobre este turno");
        }
        return item;
    }

    private void cerrarReservaSiCorresponde(Reserva reserva) {
        boolean todosCerrados = reserva.getItems().stream()
                .allMatch(i -> i.getEstado() == EstadoReservaItem.FINALIZADO || i.getEstado() == EstadoReservaItem.NO_SHOW);
        if (todosCerrados && reserva.getEstado() != EstadoReserva.CANCELADA) {
            reserva.setEstado(EstadoReserva.COMPLETADA);
            reservaRepository.save(reserva);
        }
    }

    private ReservaItemOperativoResponse toOperativoResponse(ReservaItem item) {
        List<EtapaServicioResponse> etapas = etapaServicioRepository
                .findByServicioIdOrderByOrdenAsc(item.getServicio().getId()).stream()
                .map(e -> new EtapaServicioResponse(e.getId(), e.getOrden(), e.getNombre(), e.getPorcentajeHasta()))
                .toList();

        return new ReservaItemOperativoResponse(
                item.getId(),
                item.getReserva().getId(),
                item.getReserva().getCliente().getNombre() + " " + item.getReserva().getCliente().getApellido(),
                item.getReserva().getCliente().getTelefono(),
                item.getServicio().getId(),
                item.getServicio().getNombre(),
                item.getEmpleado().getId(),
                item.getEmpleado().getNombre(),
                item.getHoraInicio(),
                item.getHoraFin(),
                item.getEstado().name(),
                item.getEtapaActual() != null ? item.getEtapaActual().getNombre() : null,
                etapas);
    }
}
