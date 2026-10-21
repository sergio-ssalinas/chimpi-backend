-- =====================================================================================
-- Seguridad en Supabase (inocuo en cualquier otro PostgreSQL).
--
-- Supabase publica por defecto las tablas del esquema "public" a traves de su API REST
-- (PostgREST), accesible con la "anon key", que es PUBLICA. Sin Row Level Security, cualquiera
-- que tenga esa clave podria leer o modificar clientes, ventas y facturas.
--
-- Con RLS activado y SIN politicas, ese acceso queda denegado. El backend no se ve afectado:
-- se conecta con el rol "postgres" (dueno de las tablas, con BYPASSRLS), que ignora RLS.
-- Si algun dia el backend usa otro rol, ese rol necesita BYPASSRLS o politicas propias.
-- Las tablas nuevas que se creen en futuras migraciones deben activar RLS tambien.
-- =====================================================================================

alter table negocios enable row level security;
alter table rubros enable row level security;
alter table empleados enable row level security;
alter table usuarios_internos enable row level security;
alter table clientes enable row level security;
alter table servicios enable row level security;
alter table servicio_componentes enable row level security;
alter table empleado_servicios enable row level security;
alter table etapas_servicio enable row level security;
alter table disponibilidad enable row level security;
alter table reservas enable row level security;
alter table reserva_items enable row level security;
alter table categorias_producto enable row level security;
alter table proveedores enable row level security;
alter table productos enable row level security;
alter table movimientos_stock enable row level security;
alter table compras enable row level security;
alter table compra_items enable row level security;
alter table cajas enable row level security;
alter table promociones enable row level security;
alter table ventas enable row level security;
alter table venta_items enable row level security;
alter table facturas enable row level security;
alter table factura_lineas enable row level security;
alter table categorias_egreso enable row level security;
alter table egresos enable row level security;
alter table socios enable row level security;
alter table pagos_socio enable row level security;
alter table if exists flyway_schema_history enable row level security;
