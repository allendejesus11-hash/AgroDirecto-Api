-- Ejecutar en la BD existente después del respaldo. No elimina registros.
ALTER TABLE carga ADD COLUMN IF NOT EXISTS latitud_destino DOUBLE PRECISION, ADD COLUMN IF NOT EXISTS longitud_destino DOUBLE PRECISION;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_carga_destino' AND conrelid='carga'::regclass) THEN
ALTER TABLE carga ADD CONSTRAINT ck_carga_destino CHECK ((latitud_destino IS NULL AND longitud_destino IS NULL) OR (latitud_destino IS NOT NULL AND longitud_destino IS NOT NULL AND latitud_destino BETWEEN -90 AND 90 AND longitud_destino BETWEEN -180 AND 180)); END IF; END $$;
-- Habilitar solicitudes para los lotes existentes sin sobrescribir moderaciones.
INSERT INTO solicitud_chat(id_productor,id_transportista,id_lote,estado_solicitud,fecha_solicitud)
SELECT DISTINCT c.id_productor,l.id_transportista,l.id_lote,'pendiente',CURRENT_TIMESTAMP FROM carga c JOIN lote l ON l.id_lote=c.id_lote WHERE l.id_transportista IS NOT NULL AND l.estado_lote IN ('formandose','confirmado','en_transito','con_retraso') AND c.estado_carga<>'cancelada'
ON CONFLICT(id_productor,id_transportista,id_lote) DO NOTHING;

ALTER TABLE precio_mercado ADD COLUMN IF NOT EXISTS fuente VARCHAR(500);
