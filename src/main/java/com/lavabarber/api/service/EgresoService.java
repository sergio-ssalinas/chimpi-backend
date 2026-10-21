package com.lavabarber.api.service;

import com.lavabarber.api.dto.backoffice.EgresoRequest;
import com.lavabarber.api.dto.backoffice.EgresoResponse;
import com.lavabarber.api.entity.*;
import com.lavabarber.api.entity.enums.EstadoCaja;
import com.lavabarber.api.entity.enums.EstadoEgreso;
import com.lavabarber.api.entity.enums.MetodoPago;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EgresoService {

    private final EgresoRepository egresoRepository;
    private final NegocioRepository negocioRepository;
    private final CategoriaEgresoRepository categoriaEgresoRepository;
    private final ProveedorRepository proveedorRepository;
    private final EmpleadoRepository empleadoRepository;
    private final CajaRepository cajaRepository;

    @Transactional
    public EgresoResponse registrar(EgresoRequest request, UsuarioInterno usuario, boolean esAdmin) {
        Negocio negocio = negocioRepository.findById(request.negocioId())
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado: " + request.negocioId()));
        CategoriaEgreso categoria = categoriaEgresoRepository.findById(request.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada: " + request.categoriaId()));

        Caja caja = null;
        if (request.cajaId() != null) {
            caja = cajaRepository.findById(request.cajaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada: " + request.cajaId()));
            if (caja.getEstado() != EstadoCaja.ABIERTA) {
                throw new BusinessException("Esa caja no esta abierta");
            }
            if (!esAdmin && !caja.getUsuarioInterno().getId().equals(usuario.getId())) {
                throw new AccessDeniedException("No podes registrar un egreso en una caja que no es tuya");
            }
        } else if (!esAdmin) {
            throw new AccessDeniedException("Solo un administrador puede registrar un egreso que no sale de una caja");
        }

        if (request.metodoPago() != MetodoPago.EFECTIVO && request.cajaId() != null) {
            throw new BusinessException("Solo los egresos en efectivo pueden atarse a una caja");
        }

        Proveedor proveedor = request.proveedorId() != null
                ? proveedorRepository.findById(request.proveedorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + request.proveedorId()))
                : null;
        Empleado empleado = request.empleadoId() != null
                ? empleadoRepository.findById(request.empleadoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + request.empleadoId()))
                : null;

        Egreso egreso = Egreso.builder()
                .negocio(negocio).categoria(categoria)
                .concepto(request.concepto()).monto(request.monto()).fecha(request.fecha())
                .metodoPago(request.metodoPago())
                .proveedor(proveedor).empleado(empleado).caja(caja)
                .comprobanteNumero(request.comprobanteNumero())
                .usuarioInterno(usuario)
                .estado(EstadoEgreso.ACTIVO)
                .build();

        return toResponse(egresoRepository.save(egreso));
    }

    @Transactional
    public EgresoResponse anular(UUID egresoId) {
        Egreso egreso = egresoRepository.findById(egresoId)
                .orElseThrow(() -> new ResourceNotFoundException("Egreso no encontrado: " + egresoId));
        if (egreso.getEstado() == EstadoEgreso.ANULADO) {
            throw new BusinessException("Este egreso ya esta anulado");
        }
        if (egreso.getCaja() != null && egreso.getCaja().getEstado() == EstadoCaja.CERRADA) {
            throw new BusinessException("No se puede anular un egreso de una caja ya cerrada");
        }
        egreso.setEstado(EstadoEgreso.ANULADO);
        return toResponse(egresoRepository.save(egreso));
    }

    @Transactional(readOnly = true)
    public List<EgresoResponse> listar(UUID negocioId) {
        return egresoRepository.findByNegocioIdOrderByFechaDesc(negocioId).stream().map(this::toResponse).toList();
    }

    private EgresoResponse toResponse(Egreso e) {
        return new EgresoResponse(
                e.getId(), e.getNegocio().getId(), e.getCategoria().getNombre(), e.getConcepto(), e.getMonto(), e.getFecha(),
                e.getMetodoPago().name(),
                e.getProveedor() != null ? e.getProveedor().getNombre() : null,
                e.getEmpleado() != null ? e.getEmpleado().getNombre() : null,
                e.getCaja() != null ? e.getCaja().getId() : null,
                e.getComprobanteNumero(),
                e.getUsuarioInterno().getNombre() + " " + e.getUsuarioInterno().getApellido(),
                e.getEstado().name());
    }
}
