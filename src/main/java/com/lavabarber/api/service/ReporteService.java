package com.lavabarber.api.service;

import com.lavabarber.api.dto.backoffice.*;
import com.lavabarber.api.entity.*;
import com.lavabarber.api.entity.enums.EstadoEgreso;
import com.lavabarber.api.entity.enums.EstadoVenta;
import com.lavabarber.api.entity.enums.MetodoPago;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reportes por rango de fechas. Todo es de solo lectura. Se agrega en memoria
 * (el volumen de un negocio chico lo permite y evita funciones de fecha
 * distintas entre H2 y PostgreSQL). Si algun dia el volumen crece, estas
 * agregaciones se pueden mover a consultas JPQL sin cambiar los DTOs.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {

    private static final int MAX_DIAS = 366;
    private static final int TOP_ITEMS = 10;

    private final VentaRepository ventaRepository;
    private final CompraRepository compraRepository;
    private final EgresoRepository egresoRepository;
    private final PagoSocioRepository pagoSocioRepository;
    private final FacturaRepository facturaRepository;

    // ---------- Resumen ----------

    public ReporteResumenResponse resumen(UUID negocioId, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);

        List<Venta> todas = ventas(negocioId, desde, hasta);
        List<Venta> ok = todas.stream().filter(v -> v.getEstado() == EstadoVenta.COMPLETADA).toList();

        BigDecimal bruto = sumar(ok, Venta::getSubtotal);
        BigDecimal descuento = sumar(ok, Venta::getDescuento);
        BigDecimal neto = sumar(ok, Venta::getTotal);
        BigDecimal ticketPromedio = ok.isEmpty()
                ? BigDecimal.ZERO
                : neto.divide(BigDecimal.valueOf(ok.size()), 0, RoundingMode.HALF_UP);

        BigDecimal compras = sumar(
                compraRepository.findByNegocioIdAndFechaBetweenOrderByFechaDesc(negocioId, desde, hasta),
                Compra::getTotal);
        BigDecimal egresos = sumar(
                egresoRepository.findByNegocioIdAndFechaBetweenOrderByFechaDesc(negocioId, desde, hasta).stream()
                        .filter(e -> e.getEstado() == EstadoEgreso.ACTIVO).toList(),
                Egreso::getMonto);
        BigDecimal pagosSocios = sumar(
                pagoSocioRepository.findByNegocioIdAndFechaBetweenOrderByFechaDesc(negocioId, desde, hasta),
                PagoSocio::getMonto);

        return new ReporteResumenResponse(
                desde, hasta,
                ok.size(), todas.size() - ok.size(),
                bruto, descuento, neto, ticketPromedio,
                compras, egresos, pagosSocios,
                neto.subtract(compras).subtract(egresos),
                porMetodo(ok), porDia(ok, desde, hasta), porTipo(ok), topItems(ok));
    }

    private List<ReporteResumenResponse.PorMetodo> porMetodo(List<Venta> ok) {
        Map<MetodoPago, List<Venta>> agrupado = ok.stream().collect(
                Collectors.groupingBy(Venta::getMetodoPago, () -> new EnumMap<>(MetodoPago.class), Collectors.toList()));
        List<ReporteResumenResponse.PorMetodo> res = new ArrayList<>();
        agrupado.forEach((metodo, lista) ->
                res.add(new ReporteResumenResponse.PorMetodo(metodo.name(), lista.size(), sumar(lista, Venta::getTotal))));
        return res;
    }

    /** Un punto por cada dia del rango (los dias sin ventas van en cero, para graficar de corrido). */
    private List<ReporteResumenResponse.PorDia> porDia(List<Venta> ok, LocalDate desde, LocalDate hasta) {
        Map<LocalDate, List<Venta>> agrupado = ok.stream()
                .collect(Collectors.groupingBy(v -> v.getFecha().toLocalDate()));
        List<ReporteResumenResponse.PorDia> res = new ArrayList<>();
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
            List<Venta> delDia = agrupado.getOrDefault(d, List.of());
            res.add(new ReporteResumenResponse.PorDia(d, delDia.size(), sumar(delDia, Venta::getTotal)));
        }
        return res;
    }

    private List<ReporteResumenResponse.PorTipo> porTipo(List<Venta> ok) {
        Map<String, BigDecimal[]> acc = new TreeMap<>(); // tipo -> [cantidad, total neto]
        for (Venta v : ok) {
            for (VentaItem it : v.getItems()) {
                BigDecimal[] a = acc.computeIfAbsent(it.getTipo().name(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                a[0] = a[0].add(it.getCantidad());
                a[1] = a[1].add(neto(it));
            }
        }
        return acc.entrySet().stream()
                .map(e -> new ReporteResumenResponse.PorTipo(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }

    private List<ReporteResumenResponse.TopItem> topItems(List<Venta> ok) {
        Map<String, BigDecimal[]> acc = new HashMap<>(); // "TIPO|descripcion" -> [cantidad, total neto]
        for (Venta v : ok) {
            for (VentaItem it : v.getItems()) {
                String clave = it.getTipo().name() + "|" + it.getDescripcion();
                BigDecimal[] a = acc.computeIfAbsent(clave, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                a[0] = a[0].add(it.getCantidad());
                a[1] = a[1].add(neto(it));
            }
        }
        return acc.entrySet().stream()
                .sorted((x, y) -> y.getValue()[1].compareTo(x.getValue()[1]))
                .limit(TOP_ITEMS)
                .map(e -> {
                    int corte = e.getKey().indexOf('|');
                    return new ReporteResumenResponse.TopItem(
                            e.getKey().substring(corte + 1), e.getKey().substring(0, corte),
                            e.getValue()[0], e.getValue()[1]);
                })
                .toList();
    }

    // ---------- Listados ----------

    public List<ReporteVentaResponse> ventasDetalle(UUID negocioId, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<Venta> ventas = ventas(negocioId, desde, hasta);

        Map<UUID, String> facturaPorVenta = new HashMap<>();
        for (Venta v : ventas) {
            facturaRepository.findByVentaId(v.getId())
                    .ifPresent(f -> facturaPorVenta.put(v.getId(), f.getNumeroFactura()));
        }

        return ventas.stream().map(v -> new ReporteVentaResponse(
                v.getId(), v.getFecha(),
                v.getCliente() != null ? v.getCliente().getNombre() + " " + v.getCliente().getApellido() : null,
                v.getUsuarioInterno().getNombre() + " " + v.getUsuarioInterno().getApellido(),
                v.getMetodoPago().name(), v.getEstado().name(),
                v.getSubtotal(), v.getDescuento(), v.getTotal(), v.getPromocionNombre(),
                facturaPorVenta.get(v.getId()),
                v.getItems().stream().map(it -> new ReporteVentaResponse.Linea(
                        it.getTipo().name(), it.getDescripcion(), it.getCantidad(),
                        it.getPrecioUnitario(), it.getSubtotal(), it.getDescuento())).toList()
        )).toList();
    }

    public List<ReporteCompraResponse> compras(UUID negocioId, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return compraRepository.findByNegocioIdAndFechaBetweenOrderByFechaDesc(negocioId, desde, hasta).stream()
                .map(c -> new ReporteCompraResponse(
                        c.getId(), c.getFecha(), c.getProveedor().getNombre(), c.getNumeroFactura(),
                        c.getItems().size(), c.getTotal()))
                .toList();
    }

    public List<ReportePagoResponse> pagos(UUID negocioId, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<ReportePagoResponse> res = new ArrayList<>();

        for (Egreso e : egresoRepository.findByNegocioIdAndFechaBetweenOrderByFechaDesc(negocioId, desde, hasta)) {
            String beneficiario = e.getProveedor() != null ? e.getProveedor().getNombre()
                    : e.getEmpleado() != null ? e.getEmpleado().getNombre() : null;
            res.add(new ReportePagoResponse(
                    e.getId(), "EGRESO", e.getFecha(), e.getCategoria().getNombre(), e.getConcepto(),
                    beneficiario, e.getMetodoPago().name(), e.getComprobanteNumero(),
                    e.getEstado().name(), e.getMonto()));
        }
        for (PagoSocio p : pagoSocioRepository.findByNegocioIdAndFechaBetweenOrderByFechaDesc(negocioId, desde, hasta)) {
            res.add(new ReportePagoResponse(
                    p.getId(), "SOCIO", p.getFecha(), "Socios", p.getConcepto(),
                    p.getSocio().getNombre(), p.getMetodoPago().name(), p.getComprobanteNumero(),
                    "ACTIVO", p.getMonto()));
        }
        res.sort(Comparator.comparing(ReportePagoResponse::fecha).reversed());
        return res;
    }

    // ---------- Helpers ----------

    private List<Venta> ventas(UUID negocioId, LocalDate desde, LocalDate hasta) {
        return ventaRepository.findByNegocioIdAndFechaGreaterThanEqualAndFechaLessThanOrderByFechaDesc(
                negocioId, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    private static BigDecimal neto(VentaItem it) {
        return it.getSubtotal().subtract(it.getDescuento());
    }

    private static <T> BigDecimal sumar(Collection<T> items, Function<T, BigDecimal> f) {
        BigDecimal total = BigDecimal.ZERO;
        for (T item : items) total = total.add(f.apply(item));
        return total;
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) throw new BusinessException("Indicar desde y hasta");
        if (hasta.isBefore(desde)) throw new BusinessException("La fecha 'hasta' no puede ser anterior a 'desde'");
        if (desde.plusDays(MAX_DIAS).isBefore(hasta)) {
            throw new BusinessException("El rango maximo es de " + MAX_DIAS + " dias");
        }
    }
}
