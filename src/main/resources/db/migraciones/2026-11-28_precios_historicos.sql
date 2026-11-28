-- Datos históricos verificados del reporte SISAP/EMMSA 15/05/2026 (ayer y hoy). Zanahoria convertida de saco de 65 kg a S/kg. No son precios actuales.
INSERT INTO precio_mercado(id_cultivo,fecha_precio,precio_por_kg,fuente)
SELECT c.id_cultivo,v.fecha,v.precio,'MIDAGRI/SISAP - EMMSA, GMML: https://cdn.www.gob.pe/uploads/document/file/9976951/8082986-reporte-de-ingreso-y-precios-en-el-gran-mercado-mayorista-de-lima-15-05-2026.pdf'
FROM (VALUES
('Papa blanca', DATE '2026-05-14', 1.400000),
('Papa blanca', DATE '2026-05-15', 1.400000),
('Cebolla roja', DATE '2026-05-14', 2.750000),
('Cebolla roja', DATE '2026-05-15', 2.830000),
('Zanahoria', DATE '2026-05-14', 0.730769),
('Zanahoria', DATE '2026-05-15', 0.557692)
) AS v(nombre,fecha,precio) JOIN cultivo c ON lower(c.nombre_cultivo)=lower(v.nombre)
ON CONFLICT(id_cultivo,fecha_precio) DO NOTHING;
