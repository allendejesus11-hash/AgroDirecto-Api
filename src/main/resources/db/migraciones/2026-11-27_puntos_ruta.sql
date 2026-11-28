-- Aplicar una vez antes de recrear la API. No elimina datos ni modifica coordenadas existentes.
-- Puede repetirse de forma segura. Ejecutar con psql -v ON_ERROR_STOP=1 -1.
ALTER TABLE public.carga
    ADD COLUMN IF NOT EXISTS latitud_acopio DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitud_acopio DOUBLE PRECISION;
ALTER TABLE public.lote
    ADD COLUMN IF NOT EXISTS latitud_origen DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitud_origen DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS latitud_destino DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitud_destino DOUBLE PRECISION;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='public.carga'::regclass AND conname='ck_carga_acopio') THEN
        ALTER TABLE public.carga ADD CONSTRAINT ck_carga_acopio CHECK ((latitud_acopio IS NULL AND longitud_acopio IS NULL) OR (latitud_acopio IS NOT NULL AND longitud_acopio IS NOT NULL AND latitud_acopio BETWEEN -90 AND 90 AND longitud_acopio BETWEEN -180 AND 180));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='public.lote'::regclass AND conname='ck_lote_origen_gps') THEN
        ALTER TABLE public.lote ADD CONSTRAINT ck_lote_origen_gps CHECK ((latitud_origen IS NULL AND longitud_origen IS NULL) OR (latitud_origen IS NOT NULL AND longitud_origen IS NOT NULL AND latitud_origen BETWEEN -90 AND 90 AND longitud_origen BETWEEN -180 AND 180));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='public.lote'::regclass AND conname='ck_lote_destino_gps') THEN
        ALTER TABLE public.lote ADD CONSTRAINT ck_lote_destino_gps CHECK ((latitud_destino IS NULL AND longitud_destino IS NULL) OR (latitud_destino IS NOT NULL AND longitud_destino IS NOT NULL AND latitud_destino BETWEEN -90 AND 90 AND longitud_destino BETWEEN -180 AND 180));
    END IF;
END;
$$;
