-- =====================================================================================
-- Chimpi Hub / lavabarber-api - esquema base (PostgreSQL)
--
-- Refleja las entidades JPA tal como estan hoy. En produccion Hibernate corre con
-- ddl-auto=validate: este archivo es el UNICO que crea el esquema. Cualquier cambio futuro
-- en una entidad (tabla, columna, nuevo valor de un enum) va en un V2__..., V3__... nuevo.
-- NUNCA se edita este archivo una vez aplicado en una base real (Flyway valida su checksum).
--
-- Convenciones (las mismas que usa Hibernate en dev):
--   * ids uuid generados por la aplicacion (sin DEFAULT).
--   * montos y cantidades numeric(19,2): misma precision que H2 en desarrollo.
--   * enums guardados como varchar con CHECK; si se agrega un valor al enum de Java hay que
--     ampliar el CHECK en una migracion nueva (ej: al sumar el rol CAJERO).
--   * documento_identidad guarda la cedula CIFRADA (AES-GCM + base64), no el numero.
-- =====================================================================================

-- ---------------------------------------------------------------- catalogo base

create table negocios (
    id                      uuid         primary key,
    nombre                  varchar(255) not null,
    direccion               varchar(255),
    telefono                varchar(255),
    ruc                     varchar(255),
    timbrado_numero         varchar(255),
    punto_expedicion        varchar(255),
    proximo_numero_factura  integer      default 1
);

create table rubros (
    id      uuid         primary key,
    nombre  varchar(255) not null,
    constraint uk_rubros_nombre unique (nombre)
);

create table empleados (
    id          uuid         primary key,
    negocio_id  uuid         not null references negocios (id),
    nombre      varchar(255) not null,
    foto_url    varchar(255),
    activo      boolean      not null default true
);
create index idx_empleados_negocio on empleados (negocio_id);

create table usuarios_internos (
    id             uuid         primary key,
    username       varchar(255) not null,
    password_hash  varchar(255) not null,
    nombre         varchar(255) not null,
    apellido       varchar(255) not null,
    rol            varchar(255) not null,
    activo         boolean      not null default true,
    empleado_id    uuid         references empleados (id),
    constraint uk_usuarios_internos_username unique (username),
    constraint ck_usuarios_internos_rol check (rol in ('ADMIN', 'EMPLEADO', 'CAJERO'))
);
create index idx_usuarios_internos_empleado on usuarios_internos (empleado_id);

create table clientes (
    id                  uuid         primary key,
    username            varchar(255) not null,
    password_hash       varchar(255) not null,
    nombre              varchar(255) not null,
    apellido            varchar(255) not null,
    direccion           varchar(255),
    fecha_nacimiento    date,
    sexo                varchar(255),
    documento_identidad varchar(255),
    telefono            varchar(255),
    ruc                 varchar(20),
    documento_hash      varchar(64),
    activo              boolean      not null default true,
    email               varchar(255),
    constraint uk_clientes_username unique (username),
    constraint uk_clientes_telefono unique (telefono),
    constraint ck_clientes_sexo check (sexo in ('MASCULINO', 'FEMENINO', 'OTRO'))
);
create index idx_clientes_documento_hash on clientes (documento_hash);

-- ---------------------------------------------------------------- servicios

create table servicios (
    id            uuid          primary key,
    negocio_id    uuid          not null references negocios (id),
    rubro_id      uuid          not null references rubros (id),
    nombre        varchar(255)  not null,
    duracion_min  integer       not null,
    precio        numeric(19,2),
    tasa_iva      varchar(255)  not null default 'DIEZ',
    constraint ck_servicios_tasa_iva check (tasa_iva in ('DIEZ', 'CINCO', 'EXENTA'))
);
create index idx_servicios_negocio on servicios (negocio_id);

create table servicio_componentes (
    servicio_id    uuid not null references servicios (id),
    componente_id  uuid not null references servicios (id),
    primary key (servicio_id, componente_id)
);

create table empleado_servicios (
    empleado_id  uuid not null references empleados (id),
    servicio_id  uuid not null references servicios (id),
    primary key (empleado_id, servicio_id)
);
create index idx_empleado_servicios_servicio on empleado_servicios (servicio_id);

create table etapas_servicio (
    id                uuid         primary key,
    servicio_id       uuid         not null references servicios (id),
    orden             integer      not null,
    nombre            varchar(255) not null,
    porcentaje_hasta  integer      not null
);
create index idx_etapas_servicio_servicio on etapas_servicio (servicio_id);

create table disponibilidad (
    id           uuid         primary key,
    empleado_id  uuid         not null references empleados (id),
    dia_semana   varchar(255) not null,
    hora_inicio  time         not null,
    hora_fin     time         not null,
    constraint ck_disponibilidad_dia check (dia_semana in
        ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
);
create index idx_disponibilidad_empleado on disponibilidad (empleado_id);

-- ---------------------------------------------------------------- reservas

create table reservas (
    id          uuid         primary key,
    cliente_id  uuid         not null references clientes (id),
    negocio_id  uuid         not null references negocios (id),
    fecha       date         not null,
    estado      varchar(255),
    creada_en   timestamp(6),
    constraint ck_reservas_estado check (estado in ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'COMPLETADA'))
);
create index idx_reservas_cliente_fecha on reservas (cliente_id, fecha);
create index idx_reservas_negocio_fecha on reservas (negocio_id, fecha);

create table reserva_items (
    id                uuid         primary key,
    reserva_id        uuid         not null references reservas (id),
    servicio_id       uuid         not null references servicios (id),
    empleado_id       uuid         not null references empleados (id),
    hora_inicio       time         not null,
    hora_fin          time         not null,
    estado            varchar(255) not null,
    etapa_actual_id   uuid         references etapas_servicio (id),
    hora_inicio_real  timestamp(6),
    hora_fin_real     timestamp(6),
    constraint ck_reserva_items_estado check (estado in
        ('PENDIENTE', 'CONFIRMADO', 'EN_PROGRESO', 'FINALIZADO', 'NO_SHOW'))
);
create index idx_reserva_items_reserva on reserva_items (reserva_id);
create index idx_reserva_items_empleado on reserva_items (empleado_id);

-- ---------------------------------------------------------------- productos y stock

create table categorias_producto (
    id          uuid         primary key,
    negocio_id  uuid         not null references negocios (id),
    nombre      varchar(255) not null
);
create index idx_categorias_producto_negocio on categorias_producto (negocio_id);

create table proveedores (
    id          uuid         primary key,
    negocio_id  uuid         not null references negocios (id),
    nombre      varchar(255) not null,
    ruc         varchar(255),
    telefono    varchar(255),
    email       varchar(255),
    direccion   varchar(255),
    activo      boolean      not null default true
);
create index idx_proveedores_negocio on proveedores (negocio_id);

create table productos (
    id             uuid          primary key,
    negocio_id     uuid          not null references negocios (id),
    categoria_id   uuid          references categorias_producto (id),
    nombre         varchar(255)  not null,
    descripcion    varchar(255),
    codigo_barras  varchar(255),
    unidad_medida  varchar(255)  not null,
    precio_venta   numeric(19,2) not null,
    tasa_iva       varchar(255)  not null,
    stock_actual   numeric(19,2) not null default 0,
    stock_minimo   numeric(19,2) not null default 0,
    activo         boolean       not null default true,
    constraint uk_productos_codigo_barras unique (codigo_barras),
    constraint ck_productos_unidad check (unidad_medida in
        ('UNIDAD', 'KILOGRAMO', 'LITRO', 'CAJA', 'PAQUETE', 'METRO')),
    constraint ck_productos_tasa_iva check (tasa_iva in ('DIEZ', 'CINCO', 'EXENTA'))
);
create index idx_productos_negocio on productos (negocio_id);

create table movimientos_stock (
    id                  uuid          primary key,
    producto_id         uuid          not null references productos (id),
    tipo                varchar(255)  not null,
    cantidad            numeric(19,2) not null,
    stock_resultante    numeric(19,2) not null,
    fecha               timestamp(6)  not null,
    motivo              varchar(255),
    usuario_interno_id  uuid          references usuarios_internos (id),
    constraint ck_movimientos_stock_tipo check (tipo in
        ('ENTRADA_COMPRA', 'SALIDA_VENTA', 'AJUSTE_POSITIVO', 'AJUSTE_NEGATIVO'))
);
create index idx_movimientos_stock_producto on movimientos_stock (producto_id, fecha);

create table compras (
    id              uuid          primary key,
    negocio_id      uuid          not null references negocios (id),
    proveedor_id    uuid          not null references proveedores (id),
    fecha           date          not null,
    numero_factura  varchar(255),
    total           numeric(19,2) not null
);
create index idx_compras_negocio_fecha on compras (negocio_id, fecha);

create table compra_items (
    id               uuid          primary key,
    compra_id        uuid          not null references compras (id),
    producto_id      uuid          not null references productos (id),
    cantidad         numeric(19,2) not null,
    precio_unitario  numeric(19,2) not null,
    subtotal         numeric(19,2) not null
);
create index idx_compra_items_compra on compra_items (compra_id);

-- ---------------------------------------------------------------- caja, ventas, facturas

create table cajas (
    id                     uuid          primary key,
    negocio_id             uuid          not null references negocios (id),
    usuario_interno_id     uuid          not null references usuarios_internos (id),
    fecha_apertura         timestamp(6)  not null,
    monto_inicial          numeric(19,2) not null,
    estado                 varchar(255)  not null,
    fecha_cierre           timestamp(6),
    monto_final_declarado  numeric(19,2),
    monto_final_esperado   numeric(19,2),
    diferencia             numeric(19,2),
    observaciones          varchar(255),
    constraint ck_cajas_estado check (estado in ('ABIERTA', 'CERRADA'))
);
create index idx_cajas_negocio on cajas (negocio_id);
create index idx_cajas_usuario_estado on cajas (usuario_interno_id, estado);

create table promociones (
    id           uuid          primary key,
    negocio_id   uuid          not null references negocios (id),
    nombre       varchar(255)  not null,
    tipo         varchar(255)  not null,
    alcance      varchar(255)  not null,
    valor        numeric(19,2),
    producto_id  uuid          references productos (id),
    servicio_id  uuid          references servicios (id),
    cliente_id   uuid          references clientes (id),
    fecha_desde  date,
    fecha_hasta  date,
    activa       boolean       not null default true,
    constraint ck_promociones_tipo check (tipo in ('PORCENTAJE', 'MONTO_FIJO', 'DOS_POR_UNO')),
    constraint ck_promociones_alcance check (alcance in ('TODA_LA_VENTA', 'PRODUCTO', 'SERVICIO'))
);
create index idx_promociones_negocio on promociones (negocio_id);

create table ventas (
    id                  uuid          primary key,
    negocio_id          uuid          not null references negocios (id),
    caja_id             uuid          not null references cajas (id),
    cliente_id          uuid          references clientes (id),
    usuario_interno_id  uuid          not null references usuarios_internos (id),
    fecha               timestamp(6)  not null,
    subtotal            numeric(19,2) not null,
    descuento           numeric(19,2) not null default 0,
    total               numeric(19,2) not null,
    promocion_id        uuid          references promociones (id),
    promocion_nombre    varchar(255),
    metodo_pago         varchar(255)  not null,
    estado              varchar(255)  not null,
    constraint ck_ventas_metodo_pago check (metodo_pago in ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA')),
    constraint ck_ventas_estado check (estado in ('COMPLETADA', 'ANULADA'))
);
create index idx_ventas_negocio_fecha on ventas (negocio_id, fecha);
create index idx_ventas_caja on ventas (caja_id);
create index idx_ventas_cliente on ventas (cliente_id);

create table venta_items (
    id               uuid          primary key,
    venta_id         uuid          not null references ventas (id),
    tipo             varchar(255)  not null,
    producto_id      uuid          references productos (id),
    servicio_id      uuid          references servicios (id),
    descripcion      varchar(255)  not null,
    cantidad         numeric(19,2) not null,
    precio_unitario  numeric(19,2) not null,
    subtotal         numeric(19,2) not null,
    descuento        numeric(19,2) not null default 0,
    constraint ck_venta_items_tipo check (tipo in ('PRODUCTO', 'SERVICIO'))
);
create index idx_venta_items_venta on venta_items (venta_id);

create table facturas (
    id               uuid          primary key,
    venta_id         uuid          not null references ventas (id),
    negocio_id       uuid          not null references negocios (id),
    numero_factura   varchar(255)  not null,
    fecha_emision    timestamp(6)  not null,
    cliente_ruc      varchar(255),
    cliente_nombre   varchar(255)  not null,
    condicion_venta  varchar(255)  not null,
    base_gravada_10  numeric(19,2) not null,
    iva_10           numeric(19,2) not null,
    base_gravada_5   numeric(19,2) not null,
    iva_5            numeric(19,2) not null,
    exenta           numeric(19,2) not null,
    total            numeric(19,2) not null,
    estado           varchar(255)  not null,
    cdc              varchar(255),
    constraint uk_facturas_venta unique (venta_id),
    constraint uk_facturas_numero unique (numero_factura),
    constraint ck_facturas_condicion check (condicion_venta in ('CONTADO', 'CREDITO')),
    constraint ck_facturas_estado check (estado in ('LOCAL', 'ENVIADA', 'APROBADA', 'RECHAZADA', 'ANULADA'))
);
create index idx_facturas_negocio_fecha on facturas (negocio_id, fecha_emision);

create table factura_lineas (
    id               uuid          primary key,
    factura_id       uuid          not null references facturas (id),
    orden            integer       not null,
    descripcion      varchar(255)  not null,
    cantidad         numeric(19,2) not null,
    precio_unitario  numeric(19,2) not null,
    descuento        numeric(19,2) not null default 0,
    importe          numeric(19,2) not null,
    tasa_iva         varchar(255)  not null,
    constraint ck_factura_lineas_tasa_iva check (tasa_iva in ('DIEZ', 'CINCO', 'EXENTA'))
);
create index idx_factura_lineas_factura on factura_lineas (factura_id, orden);

-- ---------------------------------------------------------------- egresos y socios

create table categorias_egreso (
    id          uuid         primary key,
    negocio_id  uuid         not null references negocios (id),
    nombre      varchar(255) not null
);
create index idx_categorias_egreso_negocio on categorias_egreso (negocio_id);

create table egresos (
    id                  uuid          primary key,
    negocio_id          uuid          not null references negocios (id),
    categoria_id        uuid          not null references categorias_egreso (id),
    concepto            varchar(255)  not null,
    monto               numeric(19,2) not null,
    fecha               date          not null,
    metodo_pago         varchar(255)  not null,
    proveedor_id        uuid          references proveedores (id),
    empleado_id         uuid          references empleados (id),
    caja_id             uuid          references cajas (id),
    comprobante_numero  varchar(255),
    usuario_interno_id  uuid          not null references usuarios_internos (id),
    estado              varchar(255)  not null,
    constraint ck_egresos_metodo_pago check (metodo_pago in ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA')),
    constraint ck_egresos_estado check (estado in ('ACTIVO', 'ANULADO'))
);
create index idx_egresos_negocio_fecha on egresos (negocio_id, fecha);
create index idx_egresos_caja on egresos (caja_id);

create table socios (
    id                        uuid          primary key,
    negocio_id                uuid          not null references negocios (id),
    nombre                    varchar(255)  not null,
    porcentaje_participacion  numeric(19,2) not null,
    telefono                  varchar(255),
    email                     varchar(255),
    fecha_ingreso             date,
    activo                    boolean       not null default true
);
create index idx_socios_negocio on socios (negocio_id);

create table pagos_socio (
    id                  uuid          primary key,
    socio_id            uuid          not null references socios (id),
    negocio_id          uuid          not null references negocios (id),
    monto               numeric(19,2) not null,
    fecha               date          not null,
    concepto            varchar(255)  not null,
    metodo_pago         varchar(255)  not null,
    comprobante_numero  varchar(255),
    usuario_interno_id  uuid          not null references usuarios_internos (id),
    constraint ck_pagos_socio_metodo_pago check (metodo_pago in ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA'))
);
create index idx_pagos_socio_negocio_fecha on pagos_socio (negocio_id, fecha);
create index idx_pagos_socio_socio on pagos_socio (socio_id);
