# Cambios de esquema y migraciones (Flyway)

## Estado: Flyway APLICADO
- El esquema de produccion lo crea `src/main/resources/db/migration/V1__baseline.sql` (28 tablas, reflejo de las entidades).
- Solo corre con el perfil `prod`. En `dev` sigue H2 + `create-drop` (Flyway deshabilitado).
- En `prod` Hibernate usa `ddl-auto: validate`: si una entidad no coincide con las tablas, la app no arranca.

## Como cambiar el esquema de ahora en adelante
1. Cambias la entidad (tabla, columna, nuevo valor de un enum, etc.).
2. Creas `V2__descripcion_corta.sql` (luego `V3__...`) en `db/migration/` con el `ALTER`/`CREATE` equivalente.
3. **Nunca se edita un `V*.sql` ya aplicado** (Flyway compara checksums y se niega a arrancar). Si te equivocaste, se corrige con una migracion nueva.
4. Valor nuevo en un enum guardado como varchar => hay que ampliar su CHECK. Ejemplo:
   `alter table usuarios_internos drop constraint ck_usuarios_internos_rol;`
   `alter table usuarios_internos add constraint ck_usuarios_internos_rol check (rol in ('ADMIN','EMPLEADO','CAJERO','NUEVO'));`
5. Columna NOT NULL nueva en una tabla con datos: agregala con `DEFAULT` o en tres pasos (nullable, rellenar, `set not null`).

## Primer arranque en produccion
La base nueva queda vacia: `ProdBootstrap` crea el negocio, los rubros base, las categorias de egreso y el usuario ADMIN
(variables `ADMIN_USERNAME` y `ADMIN_PASSWORD`, obligatorias solo ese primer arranque). Ver `application.yml`.

## Historial (todo incluido en V1__baseline.sql)

Decision: en desarrollo se usa H2 con `create-drop`. Las migraciones se hacen AL FINAL,
como una sola `V1__baseline.sql` generada del esquema definitivo. Esta lista sirve para
verificar que el baseline no olvide nada (cruzar contra las entidades JPA).

## Fase 4 — egresos y socios (tablas nuevas)
- `categorias_egreso`
- `egresos` (FK: negocio, categoria, proveedor?, empleado?, caja?, usuario_interno)
- `socios`
- `pagos_socio` (FK: socio, negocio, usuario_interno)

## Fase 5 — descuentos y promociones
- Tabla nueva `promociones` (FK: negocio, producto?, servicio?)
- `ventas.promocion_id` (FK, nullable), `ventas.promocion_nombre`, `ventas.descuento` (NOT NULL, default 0)
- `venta_items.descuento` (NOT NULL, default 0)

## Fase 5b — descuentos por cliente
- `promociones.cliente_id` (FK a `clientes`, nullable): promocion exclusiva de un cliente

## Fase 7 — rol CAJERO
- Sin tablas ni columnas nuevas: `usuarios_internos.rol` (varchar, enum STRING) acepta el valor nuevo `CAJERO`.
- OJO al generar el baseline: si el DDL trae un CHECK sobre `rol`, debe incluir `'ADMIN','EMPLEADO','CAJERO'`.

## Fase 8 — clientes en backoffice, RUC y factura desglosada
- `clientes.ruc` (varchar 20, nullable), `clientes.documento_hash` (varchar 64, nullable, conviene indice: busqueda exacta por cedula), `clientes.activo` (boolean NOT NULL default true)
- `clientes.telefono` pasa a **nullable** (sigue UNIQUE; Postgres permite varios NULL). Clientes cargados en mostrador pueden no tener telefono.
- `clientes.nombre/apellido`: el apellido puede ser cadena vacia (razon social de una empresa).
- Tabla nueva `factura_lineas` (FK `factura_id` NOT NULL; `orden` int, `descripcion`, `cantidad`, `precio_unitario`, `descuento`, `importe` numeric NOT NULL, `tasa_iva` varchar: DIEZ|CINCO|EXENTA)
- Si cambia `app.encryption-key`: recalcular `documento_hash` (HMAC de la cedula normalizada) ademas de re-cifrar `documento_identidad`.

## Fase 6 — reportes
- Sin cambios de esquema (solo consultas de lectura).

## Al hacer la migracion
- Cuidar tipos: UUID nativo en PostgreSQL, `numeric` para montos, `varchar` con largo explicito.
- `clientes.documento_identidad` guarda texto cifrado (AES-256): dimensionar la columna para el valor cifrado.
- Probar arrancando el backend con `ddl-auto: validate` contra una base vacia de PostgreSQL.
