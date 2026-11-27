# AgroDirecto API - paquete consolidado para AWS

Este ZIP se reconstruyo a partir del ultimo proyecto corregido que se habia entregado en la conversacion, mas la correccion de red Docker que permitio iniciar nuevamente Spring Boot en la EC2 del backend. No es una descarga directa del sistema de archivos de AWS.

## Cambios incluidos

- Endpoints de productor `/api/v1/productores/{idProductor}/cargas` y `/lotes-consolidacion`.
- Reporte administrador: separar SUM de fletes y comisiones para evitar ClassCastException `Object[]` -> `Number`.
- Esquema SQL compatible con las entidades: DOUBLE PRECISION y DNI/RUC VARCHAR(255).
- Hibernate ddl-auto=validate: no recrear ni borrar tablas.
- Seed de demo desactivado con SEED_DEMO_DATA=false.
- Docker Compose con red por defecto externa `agrodirecto_agrodirecto-network`, que coincide con el nombre de red de la EC2 discutido al solucionar el error JDBC.
- Variables del contenedor mediante `.env` local (no se incluye por seguridad). `.env.example` contiene solo valores de ejemplo.

## Despliegue en tu EC2 actual

1. Mantener el archivo `.env` que ya funciona, fuera del repositorio. NO reemplazarlo con `.env.example`.
2. Mantener el volumen PostgreSQL `agrodirecto_db_data`. No ejecutar `docker compose down -v` ni los scripts de inicializacion sobre la base actual.
3. Comprobar la red existente: `sudo docker network ls` y `sudo docker inspect db` (lista de redes).
4. En el directorio del proyecto: `sudo docker compose config --quiet`.
5. Reconstruir solamente la API: `sudo docker compose build app && sudo docker compose up -d --no-deps --force-recreate app`.
6. Verificar: `sudo docker logs --tail=40 agrodirecto-api` y `python3 scripts/verificar_despliegue.py http://localhost:8080`.

La red externa debe existir previamente, y el contenedor `db` debe estar conectado a ella. Para instalaciones desde cero en otro servidor, crearla con `sudo docker network create agrodirecto_agrodirecto-network` y, al crear los servicios con Compose, permitir que ambos se unan a la misma red.

## Notas

No se ha ejecutado una prueba real de extremo a extremo contra tu EC2 al generar este ZIP. Tu salida de logs anterior confirma que la API inicio tras la correccion de red. El ZIP conserva codigo y scripts revisados, pero el estado exacto de los archivos locales EC2 podria diferir.
