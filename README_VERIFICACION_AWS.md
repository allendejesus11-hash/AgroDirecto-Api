# AgroDirecto — revisión de integración AWS (25-11-2026)

## Hallazgos revisados

- **404 `/api/v1/productores/14/cargas`**: el frontend llama a `GET /api/v1/productores/{idProductor}/cargas`, y el controlador de ESTE proyecto lo implementa (`UsuarioController`). Si AWS devuelve 404 con `timestamp/status/error/path`, comprueba el OpenAPI de la API actualmente desplegada: posiblemente se está ejecutando un JAR o contenedor anterior. Subir únicamente el frontend a Git NO lo soluciona.
- **500 `/api/v1/reportes/transacciones/resumen`**: `ReporteServicios` ya usa `sumarFletesPorPeriodo` y `sumarComisionesPorPeriodo` por separado, evitando convertir un `Object[]` a `Number`.
- **Saldo de productor**: corregida la consulta de `LoteRepositorio` para no sumar el mismo `montoNeto` varias veces si el productor tiene dos cargas en el mismo lote.
- **PostgreSQL / Hibernate**: el script SQL incluye tipos `DOUBLE PRECISION`, `VARCHAR(255)` de DNI/RUC para coincidir con las entidades y `ddl-auto=validate`. **No volver a ejecutar el script de creación sobre la base existente**.
- **Seguridad**: los endpoints del productor requieren rol PRODUCTOR y que la ID solicitada coincida con el usuario autenticado; esperas 401/403 si falta o falla el JWT, no un 404 por la ruta.

## Paso 1 — subir a GitHub

El ZIP contiene el proyecto completo. Extrae su contenido en el directorio de trabajo del repositorio (sin crear un subdirectorio extra), revisa cambios con `git status` y haz el commit/push.

**No subas `.env` ni contraseñas**; `.env.example` solo contiene marcadores.

## Paso 2 — actualizar API en EC2 backend (conservar `.env`)

```bash
cd ~/AgroDirecto
git status                # revisar cambios locales antes de descargar
git pull --ff-only         # puede fallar si AWS tiene cambios locales; no forzar
sudo docker compose config
sudo docker compose up -d --build --force-recreate app
sudo docker logs --tail=50 agrodirecto-api
```

> **ATENCION**: tu EC2 tenía correcciones locales del esquema SQL y Docker Compose. Antes de un `git pull`, haz copia de seguridad de esos cambios y del `.env` local. No ejecutes `docker compose down -v`, porque borraría volúmenes con datos.

## Paso 3 — comprobar rutas de la versión EN EJECUCION

```bash
python3 scripts/verificar_despliegue.py http://localhost:8080
```

Si la ruta `/api/v1/productores/{idProductor}/cargas` falta, la API en ejecución NO corresponde a este código (o la construcción falló). El 404 no se arregla editando `environment.ts`.

Opcionalmente, en la propia EC2 backend:

```bash
curl -i http://localhost:8080/api/v1/productores/14/cargas
```

Sin token deberías recibir 401 o 403. Para probar el listado real, inicia sesión y utiliza la cabecera `Authorization: Bearer <token>`; comprueba que la ID sea la que devuelve el login. No compartas tokens de acceso.

## Paso 4 — revisar el 500 del administrador

Abre el panel y verifica Network -> `resumen` (respuesta 200). Si da 500, consulta:

```bash
sudo docker logs --since=5m agrodirecto-api 2>&1 | grep -E 'ERROR|Exception|ReporteServicios.java:' | tail -25
```

## Lo que aún requiere pruebas en AWS

El análisis del código y de las rutas no equivale a pruebas de extremo a extremo. No se accedió a tu EC2, PostgreSQL ni a cuentas reales para verificar todas las operaciones. No garantiza que no aparezcan errores de datos, permisos o estados de negocio.
