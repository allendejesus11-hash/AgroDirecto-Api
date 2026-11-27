-- SOLO DEMO: precios ficticios de hoy y ayer, sin reemplazar precios reales.
INSERT INTO precio_mercado(id_cultivo, precio_por_kg, fecha_precio)
SELECT c.id_cultivo, v.precio, CURRENT_DATE - v.dia FROM (VALUES
 ('Papa blanca',1.80,0),('Papa blanca',1.60,1),('Cebolla roja',2.10,0),('Cebolla roja',2.30,1),
 ('Zanahoria',1.50,0),('Zanahoria',1.50,1),('Maiz',1.20,0),('Maiz',1.10,1),('Camote',1.90,0),('Camote',2.00,1)
) v(nombre,precio,dia) JOIN cultivo c ON c.nombre_cultivo=v.nombre
ON CONFLICT(id_cultivo,fecha_precio) DO NOTHING;
