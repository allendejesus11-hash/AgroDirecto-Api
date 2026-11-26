-- Ejecutar MANUALMENTE SOLO en bases creadas con el esquema anterior.
-- Haga primero una copia de seguridad de la base PostgreSQL.
-- Alinea tipos de las entidades Java (Double, String) con PostgreSQL.
-- Esta migracion NO borra tablas ni filas. La conversion de NUMERIC a DOUBLE PRECISION
-- cambia a una representacion aproximada (posible perdida de precision decimal).
BEGIN;
ALTER TABLE public.usuario
  ALTER COLUMN dni TYPE VARCHAR(255) USING TRIM(dni),
  ALTER COLUMN ruc TYPE VARCHAR(255) USING TRIM(ruc),
  ALTER COLUMN capacidad_toneladas TYPE DOUBLE PRECISION USING capacidad_toneladas::DOUBLE PRECISION;
ALTER TABLE public.lote
  ALTER COLUMN flete_total TYPE DOUBLE PRECISION USING flete_total::DOUBLE PRECISION,
  ALTER COLUMN porcentaje_comision TYPE DOUBLE PRECISION USING porcentaje_comision::DOUBLE PRECISION,
  ALTER COLUMN monto_comision TYPE DOUBLE PRECISION USING monto_comision::DOUBLE PRECISION,
  ALTER COLUMN monto_neto TYPE DOUBLE PRECISION USING monto_neto::DOUBLE PRECISION;
ALTER TABLE public.carga
  ALTER COLUMN peso_kg TYPE DOUBLE PRECISION USING peso_kg::DOUBLE PRECISION,
  ALTER COLUMN tarifa_propuesta TYPE DOUBLE PRECISION USING tarifa_propuesta::DOUBLE PRECISION;
ALTER TABLE public.precio_mercado
  ALTER COLUMN precio_por_kg TYPE DOUBLE PRECISION USING precio_por_kg::DOUBLE PRECISION;
ALTER TABLE public.gasto_viaje
  ALTER COLUMN monto TYPE DOUBLE PRECISION USING monto::DOUBLE PRECISION;
COMMIT;
