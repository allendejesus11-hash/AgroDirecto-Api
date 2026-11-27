-- SOLO DEMOSTRACION: agrega cuatro lotes/cargas ficticios del mes actual.
-- Usa usuarios y cultivos de la semilla original; no elimina ni actualiza registros existentes.
-- Ejecutar una sola vez o repetir: ON CONFLICT evita duplicados; los códigos identifican datos demo.
BEGIN;
DO $$
BEGIN
 IF (SELECT count(*) FROM usuario WHERE correo IN ('juan.perez@agrodirecto.pe','maria.torres@agrodirecto.pe','carlos.ramirez@agrodirecto.pe','miguel.quispe@agrodirecto.pe')) <> 4
    OR NOT EXISTS (SELECT 1 FROM cultivo WHERE nombre_cultivo='Papa blanca') THEN
   RAISE EXCEPTION 'Faltan cuentas/cultivo de prueba originales; no se insertó nada';
 END IF;
END $$;
WITH ejemplos(codigo,correo,origen,monto,estado) AS (VALUES
 ('DEMO-L-001','carlos.ramirez@agrodirecto.pe','La Molina, Lima',750.0,'pagado'),
 ('DEMO-L-002','miguel.quispe@agrodirecto.pe','Huancayo, Junin',1250.0,'pagado'),
 ('DEMO-L-003','carlos.ramirez@agrodirecto.pe','Canete, Lima',1650.0,'pendiente'),
 ('DEMO-L-004','miguel.quispe@agrodirecto.pe','Ica, Ica',900.0,'pagado')
)
INSERT INTO lote(codigo_lote,id_transportista,estado_lote,origen,destino,fecha_salida,fecha_llegada,flete_total,porcentaje_comision,monto_comision,monto_neto,estado_pago,fecha_transaccion)
SELECT e.codigo,u.id_usuario,'entregado',e.origen,'Mercado Mayorista N1, Lima',CURRENT_TIMESTAMP-interval '1 day',CURRENT_TIMESTAMP-interval '1 hour',e.monto,5,round(e.monto*0.05,2),round(e.monto*0.95,2),e.estado,CURRENT_TIMESTAMP
FROM ejemplos e JOIN usuario u ON u.correo=e.correo AND u.id_rol=2
ON CONFLICT(codigo_lote) DO NOTHING;
WITH ejemplos(codigo,codigo_lote,correo,peso,tarifa) AS (VALUES
 ('DEMO-C-001','DEMO-L-001','juan.perez@agrodirecto.pe',400.0,750.0),
 ('DEMO-C-002','DEMO-L-002','maria.torres@agrodirecto.pe',600.0,1250.0),
 ('DEMO-C-003','DEMO-L-003','juan.perez@agrodirecto.pe',800.0,1650.0),
 ('DEMO-C-004','DEMO-L-004','maria.torres@agrodirecto.pe',500.0,900.0)
)
INSERT INTO carga(codigo_carga,id_productor,id_cultivo,id_lote,peso_kg,tipo_carga,direccion_destino,tarifa_propuesta,fecha_recojo,es_urgente,estado_carga)
SELECT e.codigo,u.id_usuario,c.id_cultivo,l.id_lote,e.peso,'perecible',l.destino,e.tarifa,CURRENT_DATE-1,false,'entregada'
FROM ejemplos e JOIN usuario u ON u.correo=e.correo AND u.id_rol=1 JOIN lote l ON l.codigo_lote=e.codigo_lote
JOIN cultivo c ON c.nombre_cultivo='Papa blanca'
ON CONFLICT(codigo_carga) DO NOTHING;
COMMIT;
SELECT count(*) AS transacciones, COALESCE(SUM(flete_total),0) AS total_transado, COALESCE(SUM(monto_comision),0) AS comision
FROM lote WHERE estado_pago IS NOT NULL AND fecha_transaccion>=date_trunc('month',CURRENT_TIMESTAMP) AND fecha_transaccion<date_trunc('month',CURRENT_TIMESTAMP)+interval '1 month';
