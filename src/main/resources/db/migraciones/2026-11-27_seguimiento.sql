-- Aplicar ANTES de recrear la API en una base existente. No elimina datos.
ALTER TABLE public.lote
    ADD COLUMN IF NOT EXISTS latitud DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitud DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP,
    ADD COLUMN IF NOT EXISTS motivo_retraso VARCHAR(250),
    ADD COLUMN IF NOT EXISTS eta TIMESTAMP;
