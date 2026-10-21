# LavaBarber API

Backend en Spring Boot para el sistema de reservas multi-negocio (lavadero + barbería, extensible a más rubros).

## Requisitos

- JDK 17+
- Maven 3.9+ (o usá el wrapper si lo agregás con `mvn -N io.takari:maven:wrapper`)

## Cómo correrlo

```bash
mvn spring-boot:run
```

Por defecto arranca con el perfil `dev`, que usa una base **H2 en memoria** (no necesitás instalar PostgreSQL para probar). Al arrancar vas a ver en la consola los IDs de los datos de prueba que se cargan automáticamente (negocio, servicios, empleados).

- Consola H2 (para ver las tablas): http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:lavabarber`
  - Usuario: `sa` / Password: (vacío)

## Endpoints principales

| Método | Endpoint | Auth | Descripción |
|---|---|---|---|
| POST | `/api/auth/registro` | Público | Crea la cuenta (usuario, contraseña, datos personales) y devuelve un JWT |
| POST | `/api/auth/login` | Público | Devuelve un JWT si usuario/contraseña son correctos |
| GET | `/api/auth/me` | JWT | Perfil del cliente logueado |
| GET | `/api/negocios` | Público | Lista negocios |
| GET | `/api/servicios?negocioId=...` | Público | Servicios de un negocio |
| GET | `/api/empleados?negocioId=...` | Público | Empleados de un negocio |
| GET | `/api/disponibilidad?servicioId=...&fecha=2026-08-10` | Público | Horarios libres |
| GET | `/api/disponibilidad/combo?servicioId=...&fecha=...` | Público | Horarios de un servicio Mixto (combo) |
| POST | `/api/reservas` | JWT | Crea una reserva (el cliente sale del token, no del body) |
| GET | `/api/reservas/mis-reservas` | JWT | Reservas del cliente logueado |
| POST | `/api/reservas/{id}/cancelar` | JWT | Cancela una reserva propia |

Para los endpoints marcados "JWT", mandá el header `Authorization: Bearer <token>` que te devuelve `/api/auth/login` o `/api/auth/registro`.

### Cuenta de prueba

La consola imprime al arrancar: usuario `cliente_prueba`, contraseña `clave1234`.

## Backoffice (personal interno)

Usa el mismo mecanismo de JWT que la app de clientes, pero con un tipo de cuenta distinto (`UsuarioInterno`, no `Cliente`) y un login separado. El token de backoffice lleva un claim `rol` (`ADMIN` o `EMPLEADO`) que determina qué puede hacer.

**Cuentas de prueba** (se imprimen en consola al arrancar):
- ADMIN: usuario `admin`, contraseña `admin1234`
- EMPLEADO (vinculado a Juan): usuario `juan`, contraseña `juan1234`

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| POST | `/api/backoffice/auth/login` | Público | Login de personal interno → JWT |
| GET/POST/PUT/DELETE | `/api/backoffice/usuarios` | ADMIN | Altas/bajas de cuentas del backoffice |
| GET/POST/PUT/DELETE | `/api/backoffice/rubros` | ADMIN | Mantenimiento de rubros |
| GET/POST/PUT/DELETE | `/api/backoffice/servicios` | ADMIN | Mantenimiento de servicios (incluye armar combos vía `componentesIds`) |
| GET/PUT | `/api/backoffice/servicios/{id}/etapas` | ADMIN | Etapas de progreso de un servicio |
| GET | `/api/backoffice/empleados` | ADMIN o EMPLEADO | Lista de empleados (EMPLEADO la necesita para reasignar turnos) |
| POST/PUT/DELETE | `/api/backoffice/empleados` | ADMIN | Alta/edición/baja de empleados (el DELETE desactiva, no borra) |
| GET/POST | `/api/backoffice/empleados/{id}/disponibilidad` | ADMIN | Horarios semanales de un empleado |
| DELETE | `/api/backoffice/empleados/disponibilidad/{id}` | ADMIN | Quitar una franja horaria |
| GET | `/api/backoffice/reservas?negocioId=&fecha=&empleadoId=` | ADMIN o EMPLEADO | Turnos del día. Un EMPLEADO solo ve los suyos (se fuerza el filtro server-side, ignora `empleadoId` si lo manda) |
| POST | `/api/backoffice/reservas/items/{id}/confirmar` | ADMIN o EMPLEADO (dueño) | Confirma presencia del cliente |
| PUT | `/api/backoffice/reservas/items/{id}/etapa` | ADMIN o EMPLEADO (dueño) | Avanza la etapa a mano (`{ etapaId }`) |
| POST | `/api/backoffice/reservas/items/{id}/finalizar` | ADMIN o EMPLEADO (dueño) | Marca el servicio como terminado |
| POST | `/api/backoffice/reservas/items/{id}/marcar-ausente` | ADMIN o EMPLEADO (dueño) | El cliente no se presentó |
| PUT | `/api/backoffice/reservas/items/{id}/reasignar` | ADMIN o EMPLEADO (dueño) | Cambia empleado y/u horario (`{ nuevoEmpleadoId, nuevaHoraInicio }`) — para atrasos o faltas |
| GET/POST/PUT/DELETE | `/api/backoffice/categorias-producto` | ADMIN | Categorías de producto (separadas de los Rubros de servicios) |
| GET/POST/PUT/DELETE | `/api/backoffice/productos` | ADMIN | Catálogo de productos (el DELETE desactiva, no borra) |
| GET | `/api/backoffice/productos/buscar?codigoBarras=` | ADMIN | Buscar un producto por código de barras |
| GET | `/api/backoffice/productos/{id}/movimientos` | ADMIN | Historial de movimientos de stock de un producto |
| POST | `/api/backoffice/productos/{id}/ajustar-stock` | ADMIN | Ajuste manual (`{ cantidad, motivo }` — cantidad puede ser negativa) |
| GET/POST/PUT/DELETE | `/api/backoffice/proveedores` | ADMIN | Proveedores (el DELETE desactiva, no borra) |
| GET/POST | `/api/backoffice/compras` | ADMIN | Registrar una compra (actualiza el stock automáticamente, con trazabilidad) |
| POST | `/api/backoffice/caja/abrir` | ADMIN o EMPLEADO | Abre una caja propia (`{ negocioId, montoInicial }`) — falla si ya tenés una abierta |
| GET | `/api/backoffice/caja/actual` | ADMIN o EMPLEADO | Tu caja abierta (404 si no tenés ninguna) |
| POST | `/api/backoffice/caja/{id}/cerrar` | ADMIN o EMPLEADO (dueño) | Cierra con arqueo (`{ montoFinalDeclarado, observaciones }`) |
| GET | `/api/backoffice/caja?negocioId=&estado=` | ADMIN | Lista todas las cajas del negocio |
| GET | `/api/backoffice/caja/{id}` | ADMIN o EMPLEADO (dueño) | Detalle de una caja |
| POST | `/api/backoffice/ventas` | ADMIN o EMPLEADO | Registra una venta (productos y/o servicios mezclados). El precio de cada línea lo calcula el backend desde el catálogo |
| GET | `/api/backoffice/ventas?cajaId=` | ADMIN o EMPLEADO (dueño) | Ventas de una caja |
| GET | `/api/backoffice/ventas?negocioId=` | ADMIN | Todas las ventas del negocio |
| POST | `/api/backoffice/ventas/{id}/anular` | ADMIN o EMPLEADO (dueño) | Anula una venta (restaura el stock de los productos vendidos) |
| POST | `/api/backoffice/facturas/venta/{ventaId}` | ADMIN o EMPLEADO | Genera el comprobante de una venta (`{ clienteRuc, clienteNombre, condicionVenta }`, todo opcional salvo condicionVenta) |
| GET | `/api/backoffice/facturas/venta/{ventaId}` | ADMIN o EMPLEADO | Ve la factura de una venta (404 si todavía no se generó) |
| GET | `/api/backoffice/facturas?negocioId=` | ADMIN | Lista todas las facturas del negocio |
| POST | `/api/backoffice/facturas/{id}/anular` | ADMIN | Anula una factura |
| GET/PUT | `/api/backoffice/negocio/{id}/facturacion` | ADMIN | Configuración fiscal (RUC, timbrado, punto de expedición) |

### ⚠️ Sobre la facturación (fase 3c) — leé esto antes de usarla en producción

Las facturas que genera este sistema hoy están en **"modo local"**: calculan correctamente la discriminación de IVA (10%/5%/Exenta) y la numeración con formato paraguayo, pero **no tienen validez fiscal ante la SET**. No hay timbrado electrónico real, no se firma ningún XML, y no se transmite nada al SIFEN. El campo `cdc` (Código de Control) queda `null` — ese código lo asigna el SIFEN real, nunca el sistema local.

Para que sea un comprobante fiscal válido hace falta contratar uno de dos caminos:
- **e-Kuatia'i**: el sistema gratuito de la DNIT para emisores chicos (es un portal web manual, no una API para integrar).
- **e-Kuatia**: integración real vía Web Services del SIFEN, directo o a través de un proveedor certificado (AdoqIA, EDICOM, FactAPI, entre otros).

La arquitectura (`Factura`, `EstadoFactura`, `FacturaService`) está preparada para conectar cualquiera de esos proveedores el día que Chimpi Hub tenga RUC y timbrado — solo hay que agregar el adaptador que hable con su API real; el resto del sistema (cálculo de IVA, numeración, UI) no debería necesitar cambios grandes.

**Sobre el progreso (fase 2)**: en cuanto un empleado toca cualquiera de los endpoints de arriba sobre un turno, ese turno pasa a usar el estado marcado a mano para calcular el `%` que ve el cliente en `/api/progreso/*`, en vez de la simulación por tiempo transcurrido. Un turno que nadie tocó (`PENDIENTE`) sigue usando la simulación automática como antes — no rompe nada de lo que ya había.

**Sobre el stock (fase 3)**: `Producto.stockActual` **nunca** se edita directo — todo pasa por `StockService`, que además deja un `MovimientoStock` por cada cambio (entrada por compra, ajuste manual positivo o negativo). Esto da trazabilidad completa: siempre se puede reconstruir por qué el stock de un producto quedó en tal número. El endpoint de ajuste manual rechaza operaciones que dejarían el stock en negativo.

**Nota de diseño**: los DELETE de Empleado y UsuarioInterno son *soft delete* (marcan `activo: false`) en vez de borrar la fila — así no se rompe el historial de reservas ya hechas ni la trazabilidad de quién hizo qué. Rubro y Servicio sí borran de verdad, pero fallan con un mensaje claro (409) si hay datos que dependen de ellos.

**CORS**: habilitado para que un frontend web (React, Vue, etc.) en otro origen pueda consumir la API. Orígenes permitidos configurables via `app.cors-allowed-origins` (por defecto `http://localhost:3000,http://localhost:5173` en dev; obligatorio definir `CORS_ALLOWED_ORIGINS` en producción).

### Sobre la cédula (documento de identidad)

Es un campo opcional en el registro. Se guarda **encriptada** en la base (AES-256, ver `security/DocumentoIdentidadConverter.java`) — nunca en texto plano, y el endpoint `/api/auth/me` solo informa si el cliente *tiene* documento cargado (`tieneDocumentoCargado: true/false`), no el número completo. Cuando armes el backoffice, restringí bien quién puede consultarlo.

### Variables de entorno para producción

```bash
export JWT_SECRET=$(openssl rand -base64 32)
export APP_ENCRYPTION_KEY=$(openssl rand -base64 32)
```

Sin estas variables, `prod` no arranca (a propósito — evita quedar con las claves de desarrollo puestas en un servidor real).

## Ejemplo: login y reservar corte + lavado en simultáneo

```bash
# 1) Login (o /api/auth/registro si es la primera vez)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"cliente_prueba","password":"clave1234"}' | jq -r '.token')

# 2) Crear la reserva usando el token (el cliente sale del token, no del body)
curl -X POST http://localhost:8080/api/reservas \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "negocioId": "UUID_DEL_NEGOCIO",
    "fecha": "2026-08-10",
    "items": [
      { "servicioId": "UUID_CORTE", "empleadoId": "UUID_JUAN", "horaInicio": "10:00" },
      { "servicioId": "UUID_LAVADO", "empleadoId": "UUID_PEDRO", "horaInicio": "10:00" }
    ]
  }'
```

Reemplazá los UUIDs de negocio/servicio/empleado por los que imprime la consola al arrancar (requiere `jq` instalado; si no lo tenés, copiá el `token` del primer `curl` a mano).

## Pasar a producción (PostgreSQL)

```bash
export DB_URL=jdbc:postgresql://TU_HOST:5432/lavabarber
export DB_USER=tu_usuario
export DB_PASSWORD=tu_password
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

En `prod`, `ddl-auto` está en `validate` (no crea tablas solo). Para eso conviene agregar Flyway o Liquibase antes de ir a producción real — avisame si querés que lo sumemos.

## Próximos pasos sugeridos

1. Agregar Flyway para migraciones versionadas de base de datos.
2. Agregar Spring Security + JWT para autenticación de clientes/admin.
3. Endpoint de administración (alta de negocios, servicios, empleados, disponibilidad) — hoy solo están los de lectura + el POST de reservas.
4. Notificaciones (Firebase Cloud Messaging) al confirmar/cancelar turno.
5. Conectar la app Flutter a estos endpoints.
