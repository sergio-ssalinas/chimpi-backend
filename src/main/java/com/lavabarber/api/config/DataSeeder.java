package com.lavabarber.api.config;

import com.lavabarber.api.entity.*;
import com.lavabarber.api.entity.enums.Sexo;
import com.lavabarber.api.entity.enums.EstadoReserva;
import com.lavabarber.api.entity.enums.RolInterno;
import com.lavabarber.api.entity.enums.TasaIva;
import com.lavabarber.api.entity.enums.TipoMovimientoStock;
import com.lavabarber.api.entity.enums.UnidadMedida;
import com.lavabarber.api.repository.*;
import com.lavabarber.api.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;

/**
 * Carga datos de ejemplo SOLO en el perfil "dev" (base H2 en memoria),
 * para poder probar la API sin cargar todo a mano. No se ejecuta en "prod".
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final NegocioRepository negocioRepository;
    private final RubroRepository rubroRepository;
    private final ServicioRepository servicioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final ClienteRepository clienteRepository;
    private final EtapaServicioRepository etapaServicioRepository;
    private final UsuarioInternoRepository usuarioInternoRepository;
    private final CategoriaProductoRepository categoriaProductoRepository;
    private final com.lavabarber.api.security.DocumentoHasher documentoHasher;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final StockService stockService;
    private final CajaRepository cajaRepository;
    private final VentaRepository ventaRepository;
    private final FacturaRepository facturaRepository;
    private final CategoriaEgresoRepository categoriaEgresoRepository;
    private final EgresoRepository egresoRepository;
    private final SocioRepository socioRepository;
    private final PagoSocioRepository pagoSocioRepository;
    private final ReservaRepository reservaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Negocio negocio = negocioRepository.save(Negocio.builder()
                .nombre("Chimpi Hub")
                .direccion("Av. Principal 123, Itaugua")
                .telefono("0981000000")
                .build());

        Rubro barberia = rubroRepository.save(Rubro.builder().nombre("Barberia").build());
        Rubro lavadero = rubroRepository.save(Rubro.builder().nombre("Lavadero").build());
        Rubro mixto = rubroRepository.save(Rubro.builder().nombre("Mixto").build());

        Servicio corte = servicioRepository.save(Servicio.builder()
                .negocio(negocio).rubro(barberia).nombre("Corte de pelo")
                .duracionMinutos(30).precio(new BigDecimal("50000")).build());

        Servicio lavado = servicioRepository.save(Servicio.builder()
                .negocio(negocio).rubro(lavadero).nombre("Lavado completo")
                .duracionMinutos(45).precio(new BigDecimal("70000")).build());

        Servicio comboVip = servicioRepository.save(Servicio.builder()
                .negocio(negocio).rubro(mixto).nombre("Combo corte + lavado VIP")
                .duracionMinutos(60).precio(new BigDecimal("110000")).build());

        // Etapas de progreso: el % es acumulado, no el peso de cada tramo.
        etapaServicioRepository.save(EtapaServicio.builder().servicio(corte).orden(1).nombre("Ingreso").porcentajeHasta(15).build());
        etapaServicioRepository.save(EtapaServicio.builder().servicio(corte).orden(2).nombre("Corte en curso").porcentajeHasta(90).build());
        etapaServicioRepository.save(EtapaServicio.builder().servicio(corte).orden(3).nombre("Finalizando").porcentajeHasta(100).build());

        etapaServicioRepository.save(EtapaServicio.builder().servicio(lavado).orden(1).nombre("Ingreso").porcentajeHasta(10).build());
        etapaServicioRepository.save(EtapaServicio.builder().servicio(lavado).orden(2).nombre("Lavado").porcentajeHasta(60).build());
        etapaServicioRepository.save(EtapaServicio.builder().servicio(lavado).orden(3).nombre("Secado").porcentajeHasta(90).build());
        etapaServicioRepository.save(EtapaServicio.builder().servicio(lavado).orden(4).nombre("Entrega").porcentajeHasta(100).build());

        // El combo se arma a partir de sus componentes atomicos: cada uno
        // necesita su propio empleado, y se reservan en simultaneo.
        comboVip.getComponentes().add(corte);
        comboVip.getComponentes().add(lavado);
        comboVip = servicioRepository.save(comboVip);

        Empleado juan = Empleado.builder().negocio(negocio).nombre("Juan (barbero)").activo(true).build();
        juan.getServicios().add(corte);
        juan = empleadoRepository.save(juan);

        Empleado pedro = Empleado.builder().negocio(negocio).nombre("Pedro (lavadero)").activo(true).build();
        pedro.getServicios().add(lavado);
        pedro = empleadoRepository.save(pedro);

        for (DayOfWeek dia : new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY}) {
            disponibilidadRepository.save(Disponibilidad.builder()
                    .empleado(juan).diaSemana(dia).horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(18, 0)).build());
            disponibilidadRepository.save(Disponibilidad.builder()
                    .empleado(pedro).diaSemana(dia).horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(17, 0)).build());
        }

        Cliente clientePrueba = clienteRepository.save(Cliente.builder()
                .username("cliente_prueba")
                .passwordHash(passwordEncoder.encode("clave1234"))
                .nombre("Cliente")
                .apellido("De Prueba")
                .direccion("Calle Falsa 123, Itaugua")
                .fechaNacimiento(LocalDate.of(1995, 5, 20))
                .sexo(Sexo.MASCULINO)
                .documentoIdentidad("4123456")
                .documentoHash(documentoHasher.hash("4123456"))
                .ruc("4123456-0")
                .telefono("0987654321")
                .email("cliente@test.com")
                .build());

        System.out.println("=== Login de prueba: usuario 'cliente_prueba' / clave 'clave1234' ===");

        // Reserva de ejemplo "en curso ahora mismo", para poder ver el
        // progreso funcionando apenas se levanta el backend, sin esperar.
        LocalTime hace10Min = LocalTime.now().minusMinutes(10);
        Reserva reservaDemo = Reserva.builder()
                .cliente(clientePrueba)
                .negocio(negocio)
                .fecha(LocalDate.now())
                .estado(EstadoReserva.CONFIRMADA)
                .build();
        ReservaItem itemDemo = ReservaItem.builder()
                .reserva(reservaDemo)
                .servicio(lavado)
                .empleado(pedro)
                .horaInicio(hace10Min)
                .horaFin(hace10Min.plusMinutes(45))
                .build();
        reservaDemo.setItems(java.util.List.of(itemDemo));
        reservaRepository.save(reservaDemo);
        System.out.println("Reserva demo 'en progreso' creada para ver /api/progreso/activo funcionando.");

        // Usuario admin del backoffice.
        UsuarioInterno admin = usuarioInternoRepository.save(UsuarioInterno.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode("admin1234"))
                .nombre("Sergio")
                .apellido("Salinas")
                .rol(RolInterno.ADMIN)
                .activo(true)
                .build());
        System.out.println("=== Login backoffice: usuario 'admin' / clave 'admin1234' ===");

        // Usuario EMPLEADO del backoffice, vinculado a Juan (para probar la
        // vista operativa del dia: solo deberia ver sus propios turnos).
        usuarioInternoRepository.save(UsuarioInterno.builder()
                .username("juan")
                .passwordHash(passwordEncoder.encode("juan1234"))
                .nombre("Juan")
                .apellido("Barbero")
                .rol(RolInterno.EMPLEADO)
                .activo(true)
                .empleado(juan)
                .build());
        System.out.println("=== Login backoffice (empleado): usuario 'juan' / clave 'juan1234' ===");

        // Usuario CAJERO del backoffice: solo ve Caja y POS.
        usuarioInternoRepository.save(UsuarioInterno.builder()
                .username("cajero")
                .passwordHash(passwordEncoder.encode("cajero1234"))
                .nombre("Carla")
                .apellido("Cajera")
                .rol(RolInterno.CAJERO)
                .activo(true)
                .build());
        System.out.println("=== Login backoffice (cajero): usuario 'cajero' / clave 'cajero1234' ===");

        // --- Productos y stock ---
        CategoriaProducto categoriaBarberia = categoriaProductoRepository.save(
                CategoriaProducto.builder().negocio(negocio).nombre("Insumos de barberia").build());
        CategoriaProducto categoriaLavadero = categoriaProductoRepository.save(
                CategoriaProducto.builder().negocio(negocio).nombre("Insumos de lavadero").build());

        Producto gel = productoRepository.save(Producto.builder()
                .negocio(negocio).categoria(categoriaBarberia).nombre("Gel para cabello")
                .codigoBarras("7891000100103").unidadMedida(UnidadMedida.UNIDAD)
                .precioVenta(new BigDecimal("35000")).tasaIva(TasaIva.DIEZ)
                .stockMinimo(new BigDecimal("5")).activo(true).build());

        Producto shampooAuto = productoRepository.save(Producto.builder()
                .negocio(negocio).categoria(categoriaLavadero).nombre("Shampoo para auto (1L)")
                .codigoBarras("7891000200203").unidadMedida(UnidadMedida.LITRO)
                .precioVenta(new BigDecimal("28000")).tasaIva(TasaIva.DIEZ)
                .stockMinimo(new BigDecimal("3")).activo(true).build());

        Producto aromatizante = productoRepository.save(Producto.builder()
                .negocio(negocio).categoria(categoriaLavadero).nombre("Aromatizante para auto")
                .unidadMedida(UnidadMedida.UNIDAD)
                .precioVenta(new BigDecimal("15000")).tasaIva(TasaIva.CINCO)
                .stockMinimo(new BigDecimal("10")).activo(true).build());

        Proveedor proveedor = proveedorRepository.save(Proveedor.builder()
                .negocio(negocio).nombre("Distribuidora Paraguay S.A.")
                .ruc("80012345-6").telefono("021555000").activo(true).build());

        // Entrada de stock inicial via StockService (no seteando stockActual a
        // mano), para que quede el MovimientoStock correspondiente.
        stockService.registrarMovimiento(gel, TipoMovimientoStock.ENTRADA_COMPRA, new BigDecimal("20"),
                "Stock inicial - Distribuidora Paraguay S.A.", admin);
        stockService.registrarMovimiento(shampooAuto, TipoMovimientoStock.ENTRADA_COMPRA, new BigDecimal("15"),
                "Stock inicial - Distribuidora Paraguay S.A.", admin);
        stockService.registrarMovimiento(aromatizante, TipoMovimientoStock.ENTRADA_COMPRA, new BigDecimal("8"),
                "Stock inicial - Distribuidora Paraguay S.A.", admin);
        System.out.println("Productos y stock inicial cargados. Proveedor ID: " + proveedor.getId());

        // --- Caja y venta de ejemplo ---
        Caja cajaDemo = cajaRepository.save(Caja.builder()
                .negocio(negocio).usuarioInterno(admin)
                .fechaApertura(java.time.LocalDateTime.now())
                .montoInicial(new BigDecimal("500000"))
                .estado(com.lavabarber.api.entity.enums.EstadoCaja.ABIERTA)
                .build());

        // Venta mezclando un producto (descuenta stock) y un servicio.
        BigDecimal subtotalGel = gel.getPrecioVenta();
        BigDecimal subtotalCorte = corte.getPrecio();
        Venta ventaDemo = Venta.builder()
                .negocio(negocio).caja(cajaDemo).usuarioInterno(admin)
                .fecha(java.time.LocalDateTime.now())
                .metodoPago(com.lavabarber.api.entity.enums.MetodoPago.EFECTIVO)
                .estado(com.lavabarber.api.entity.enums.EstadoVenta.COMPLETADA)
                .subtotal(subtotalGel.add(subtotalCorte))
                .total(subtotalGel.add(subtotalCorte))
                .build();
        ventaDemo.setItems(java.util.List.of(
                VentaItem.builder().venta(ventaDemo).tipo(com.lavabarber.api.entity.enums.TipoItemVenta.PRODUCTO)
                        .producto(gel).descripcion(gel.getNombre()).cantidad(BigDecimal.ONE)
                        .precioUnitario(gel.getPrecioVenta()).subtotal(subtotalGel).build(),
                VentaItem.builder().venta(ventaDemo).tipo(com.lavabarber.api.entity.enums.TipoItemVenta.SERVICIO)
                        .servicio(corte).descripcion(corte.getNombre()).cantidad(BigDecimal.ONE)
                        .precioUnitario(subtotalCorte).subtotal(subtotalCorte).build()));
        ventaRepository.save(ventaDemo);
        stockService.registrarMovimiento(gel, TipoMovimientoStock.SALIDA_VENTA, BigDecimal.ONE, "Venta", admin);

        System.out.println("Caja demo abierta (admin, fondo Gs. 500.000) con una venta de ejemplo (gel + corte).");

        // Factura "modo local" de ejemplo. El negocio arranca sin RUC/timbrado
        // cargados a proposito (asi es la realidad de Chimpi Hub hoy) — la
        // numeracion usa el punto de expedicion por defecto "001-001".
        BigDecimal baseGel = gel.getPrecioVenta().divide(new BigDecimal("1.10"), 0, java.math.RoundingMode.HALF_UP);
        BigDecimal ivaGel = gel.getPrecioVenta().subtract(baseGel);
        BigDecimal baseCorte = corte.getPrecio().divide(new BigDecimal("1.10"), 0, java.math.RoundingMode.HALF_UP);
        BigDecimal ivaCorte = corte.getPrecio().subtract(baseCorte);

        Factura facturaDemo = Factura.builder()
                .venta(ventaDemo).negocio(negocio)
                .numeroFactura("001-001-0000001")
                .fechaEmision(java.time.LocalDateTime.now())
                .clienteNombre("Consumidor Final")
                .condicionVenta(com.lavabarber.api.entity.enums.CondicionVenta.CONTADO)
                .baseGravada10(baseGel.add(baseCorte)).iva10(ivaGel.add(ivaCorte))
                .baseGravada5(BigDecimal.ZERO).iva5(BigDecimal.ZERO)
                .exenta(BigDecimal.ZERO)
                .total(ventaDemo.getTotal())
                .estado(com.lavabarber.api.entity.enums.EstadoFactura.LOCAL)
                .build();
        facturaRepository.save(facturaDemo);
        negocio.setProximoNumeroFactura(2);
        negocioRepository.save(negocio);
        System.out.println("Factura demo generada (modo local, sin timbrado real): 001-001-0000001");

        // --- Egresos de ejemplo ---
        com.lavabarber.api.entity.CategoriaEgreso catInsumos = categoriaEgresoRepository.save(
                com.lavabarber.api.entity.CategoriaEgreso.builder().negocio(negocio).nombre("Insumos").build());
        com.lavabarber.api.entity.CategoriaEgreso catSueldos = categoriaEgresoRepository.save(
                com.lavabarber.api.entity.CategoriaEgreso.builder().negocio(negocio).nombre("Sueldos").build());
        categoriaEgresoRepository.save(com.lavabarber.api.entity.CategoriaEgreso.builder().negocio(negocio).nombre("Servicios publicos").build());
        categoriaEgresoRepository.save(com.lavabarber.api.entity.CategoriaEgreso.builder().negocio(negocio).nombre("Alquiler").build());

        egresoRepository.save(com.lavabarber.api.entity.Egreso.builder()
                .negocio(negocio).categoria(catInsumos)
                .concepto("Compra de toallas descartables").monto(new BigDecimal("85000"))
                .fecha(LocalDate.now()).metodoPago(com.lavabarber.api.entity.enums.MetodoPago.EFECTIVO)
                .caja(cajaDemo).usuarioInterno(admin)
                .estado(com.lavabarber.api.entity.enums.EstadoEgreso.ACTIVO)
                .build());

        egresoRepository.save(com.lavabarber.api.entity.Egreso.builder()
                .negocio(negocio).categoria(catSueldos)
                .concepto("Adelanto de sueldo - Juan").monto(new BigDecimal("400000"))
                .fecha(LocalDate.now()).metodoPago(com.lavabarber.api.entity.enums.MetodoPago.TRANSFERENCIA)
                .empleado(juan).usuarioInterno(admin)
                .estado(com.lavabarber.api.entity.enums.EstadoEgreso.ACTIVO)
                .build());
        System.out.println("Egresos de ejemplo cargados (uno en efectivo desde la caja demo, otro por transferencia).");

        // --- Socios de ejemplo ---
        Socio socioSergio = socioRepository.save(Socio.builder()
                .negocio(negocio).nombre("Sergio Salinas")
                .porcentajeParticipacion(new BigDecimal("70"))
                .fechaIngreso(LocalDate.now()).activo(true).build());
        socioRepository.save(Socio.builder()
                .negocio(negocio).nombre("Socio Inversor")
                .porcentajeParticipacion(new BigDecimal("30"))
                .fechaIngreso(LocalDate.now()).activo(true).build());

        pagoSocioRepository.save(PagoSocio.builder()
                .socio(socioSergio).negocio(negocio)
                .monto(new BigDecimal("200000")).fecha(LocalDate.now())
                .concepto("Adelanto de utilidades")
                .metodoPago(com.lavabarber.api.entity.enums.MetodoPago.TRANSFERENCIA)
                .usuarioInterno(admin)
                .build());
        System.out.println("Socios de ejemplo cargados (70% / 30%) con un pago de utilidades de muestra.");

        System.out.println("=== Datos de prueba cargados ===");
        System.out.println("Negocio ID: " + negocio.getId());
        System.out.println("Servicio 'Corte de pelo' ID: " + corte.getId());
        System.out.println("Servicio 'Lavado completo' ID: " + lavado.getId());
        System.out.println("Servicio 'Combo VIP' ID: " + comboVip.getId());
        System.out.println("Empleado Juan ID: " + juan.getId());
        System.out.println("Empleado Pedro ID: " + pedro.getId());
    }
}
