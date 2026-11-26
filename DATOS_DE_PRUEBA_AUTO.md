# Carga automática de datos de prueba (solo desarrollo)

Al arrancar Spring Boot, Hibernate (`spring.jpa.hibernate.ddl-auto=update`) crea las tablas si faltan. Luego `DatosPruebaInitializer` comprueba **las 11 tablas**.

- **Todas vacías:** ejecuta `src/main/resources/db/datos_prueba_postgresql_bcrypt.sql` en una transacción, agrega los datos de prueba y completa fechas de auditoría.
- **Alguna tabla contiene registros:** no ejecuta el script ni borra nada. Esto protege registros reales y evita que se vuelvan a cargar datos duplicados al reiniciar.
- **Producción:** definir `SEED_DEMO_DATA=false` (o `app.seed-demo-data=false`). Por defecto está habilitado para pruebas locales.

**Importante:** el script original contiene `TRUNCATE ... RESTART IDENTITY CASCADE` y emplea ID fijos. No se debe ejecutar directamente sobre bases con información valiosa. El inicializador únicamente lo ejecuta cuando ha confirmado que las 11 tablas están vacías.

## Qué hacer ahora

1. Descomprimir el ZIP actualizado y ejecutar `AgroDirectoApiApplication` desde IntelliJ.
2. Revisar el log `Datos de prueba: carga completada` y refrescar `Schemas > public > Tables` en pgAdmin.
3. Comprobar los usuarios: `SELECT id_usuario, correo, estado_cuenta FROM usuario ORDER BY id_usuario;`
4. Ejemplo de acceso: `juan.perez@agrodirecto.pe` con contraseña `Agro2026*` (cuenta de demostración).

Si el log indica **se omite la carga**, alguna tabla ya tenía filas (a veces solo `rol` o `cultivo` creadas por el script del esquema); el inicializador no borra esos datos. Para cargar la demo en una base vacía de manera segura, crear una **nueva base de desarrollo**, apuntar `spring.datasource.url` a ella y dejar que Hibernate la cree de cero.

Cambios respecto a la versión anterior: solo se añadió la carga segura de ejemplos, la propiedad de control y fechas de auditoría para datos de prueba. No se cambiaron entidades, controladores, endpoints, JWT ni el frontend.
