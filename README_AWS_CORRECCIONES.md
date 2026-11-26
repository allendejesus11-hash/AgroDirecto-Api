# AgroDirecto API — versión corregida para AWS

Proyecto Spring Boot 4.0.8 / Java 21 / PostgreSQL 16, con código Java, `pom.xml`, scripts SQL y Docker.

## Cambios consolidados

- **Reporte administrador 500**: `ReporteServicios.resumenTransacciones()` utiliza **dos agregados separados** de `LoteRepositorio` (`sumarFletesPorPeriodo` y `sumarComisionesPorPeriodo`), evitando convertir un `Object[]` a `Number`.
- **Esquema PostgreSQL**: 9 columnas `DECIMAL` pasan a `DOUBLE PRECISION` para coincidir con tipos `Double` actuales; `usuario.dni` y `usuario.ruc` pasan a `VARCHAR(255)` para coincidir con `String` actual.
- **JPA**: `ddl-auto=validate` por defecto, no `update` ni `create-drop`; los datos de demostración por `ApplicationRunner` quedan desactivados por defecto.
- **Docker**: build desde el raíz del proyecto, API `8080:8088`, PostgreSQL `5433:5432`, scripts en `/docker-entrypoint-initdb.d` solamente para volumen de base **nuevo y vacío**.

## Levantar en una instalación NUEVA

1. Copia `.env.example` como `.env` y reemplaza `POSTGRES_PASSWORD` y `JWT_SECRET` con valores seguros.
2. Ejecuta `docker compose up -d --build`.
3. Comprueba con `docker logs --tail=50 agrodirecto-api`, y abre `http://localhost:8080/swagger-ui/index.html`.

**MUY IMPORTANTE, instancia EC2 existente:** No borres el volumen ni vuelvas a ejecutar los scripts de datos. No sobrescribas un `.env` o `docker-compose.yaml` en uso sin revisar antes credenciales, volúmenes y puertos. Para PostgreSQL con un volumen existente, usa el password real original, ya que cambiar `POSTGRES_PASSWORD` en Compose NO modifica la contraseña guardada en PostgreSQL.

## Esquema y datos

- `src/main/resources/db/agrodirecto_schema_postgresql.sql`: crea tablas compatibles con Java **en una BD nueva**.
- `src/main/resources/db/datos_prueba_postgresql_bcrypt.sql`: carga datos de demostración; puede contener `TRUNCATE` y **no debe ejecutarse** sobre una base con datos que quieras conservar.
- `src/main/resources/db/migraciones/2026-11-25_alinear_tipos.sql`: migración manual del esquema antiguo; NO se ejecuta automáticamente. Antes de usarla, realiza respaldo y comprueba si las columnas ya están migradas.

### Nota técnica

`Double` / `DOUBLE PRECISION` es de precisión aproximada. Para importes monetarios en una evolución futura conviene migrar de manera coordinada a `BigDecimal` / `NUMERIC`, sin combinar tipos de distintas versiones.

## Integración frontend

- URL API pública por defecto en AWS: `http://44.213.38.214:8080`.
- Frontend Docker: el proxy `/api` envía peticiones a ese backend. 
- Si el backend falla en runtime, consulta el stacktrace en sus logs. El arranque correcto de Spring Boot no garantiza que todos los endpoints estén probados.

**Limitación de esta entrega:** el proyecto se reconstruye a partir de los ZIP disponibles y los cambios documentados en la conversación, no se extrajeron los archivos directamente de las instancias EC2 actuales. No se ha realizado prueba de extremo a extremo con tu base en AWS en este entorno.
