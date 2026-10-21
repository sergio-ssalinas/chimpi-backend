# Desplegar en Render con base en Supabase

## Secretos: que va a GitHub y que no
- **A GitHub va el codigo y `render.yaml`**. En `render.yaml` ninguna clave tiene valor (`sync: false`).
- **Nunca se suben** claves, contrasenas ni cadenas de conexion: ni en `application.yml`, ni en un `.env`, ni en un comentario. El `.gitignore` ya excluye `.env*`.
- Los valores reales viven **solo en el panel de Render** (Environment) y en tu gestor de contrasenas.
- Si alguna vez un secreto llego a GitHub, aunque lo borres despues queda en el historial: hay que **cambiarlo** (nueva contrasena de la base en Supabase, nuevo `JWT_SECRET`, etc.). La unica excepcion es `APP_ENCRYPTION_KEY`, que no se puede cambiar sin re-cifrar las cedulas: en ese caso consultar antes.

## Supabase
1. En el panel de Supabase: **Connect > Session pooler** (puerto 5432). Render solo tiene IPv4 y la conexion directa de Supabase es IPv6; el Session pooler funciona por IPv4. No uses el Transaction pooler (6543): no soporta sentencias preparadas y Flyway usa bloqueos de sesion.
2. Arma las variables de Render:
   - `DB_URL` = `jdbc:postgresql://aws-0-REGION.pooler.supabase.com:5432/postgres?sslmode=require`
   - `DB_USER` = `postgres.REFERENCIA_DEL_PROYECTO`
   - `DB_PASSWORD` = la contrasena de la base (si tiene caracteres raros no hay que codificarla: va en una variable aparte, no dentro de la URL)
3. La migracion `V2__habilitar_rls.sql` activa Row Level Security en todas las tablas, para que la API publica de Supabase (anon key) no pueda leer ni escribir tus datos. El backend sigue funcionando porque usa el rol `postgres`.

## Pasos en Render
1. Subi `lavabarber-api` a GitHub.
2. **New > Blueprint**, elegi el repositorio, y completa las variables que pide: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `APP_ENCRYPTION_KEY`, `CORS_ALLOWED_ORIGINS`, `NEGOCIO_NOMBRE`, `ADMIN_USERNAME`, `ADMIN_PASSWORD` (10+ caracteres).
   (Si preferis no usar Blueprint: **New > Web Service**, runtime Docker, y cargas las mismas variables a mano.)
3. El primer arranque aplica V1 y V2 y crea negocio, rubros y admin (`ProdBootstrap`). Espera en los logs `Started LavabarberApiApplication`.
4. Probar: `https://TU-SERVICIO.onrender.com/api/negocios`.
5. Despues del primer arranque, saca `ADMIN_PASSWORD` de las variables.

## Cuando conectes los clientes
- Backoffice: `VITE_API_BASE_URL=https://TU-SERVICIO.onrender.com/api` antes de `npm run build`; el dominio donde lo publiques va en `CORS_ALLOWED_ORIGINS` (varios, separados por coma).
- Flutter: `flutter build apk --dart-define=API_BASE_URL=https://TU-SERVICIO.onrender.com/api`

## Limites del plan free de Render (verificar en render.com/pricing)
- Se duerme a los 15 min sin trafico (el primer pedido tarda ~1 min) y tiene 512 MB de RAM: justo para Spring Boot.
- Para uso real: plan Starter (siempre encendido).

## Backups
Supabase hace backups segun el plan; revisa su retencion. Ademas, `pg_dump` desde tu PC con la conexion directa o el Session pooler:
`pg_dump "postgresql://USUARIO:CLAVE@HOST:5432/postgres" -Fc -f chimpi_$(date +%F).dump`

## Importante
- `APP_ENCRYPTION_KEY`: se elige una vez y se guarda en un lugar seguro. Si se pierde o cambia, las cedulas guardadas no se pueden recuperar.
