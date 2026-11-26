# Ajustes mínimos para integración de Angular

Se agregaron dos endpoints **con verificación de propiedad** para el usuario productor:

- `GET /api/v1/productores/{idProductor}/cargas`: sus cargas, incluidas las todavía no agrupadas, para HU-02.
- `GET /api/v1/productores/{idProductor}/lotes-consolidacion`: sus lotes y las cargas agrupadas (HU-11).

No se abrió `GET /api/v1/cargas` a productores, porque expondría cargas de otros usuarios; permanece exclusivo de administradores como antes. Se conservan los roles Spring Security y los 21 controladores existentes. Las credenciales de PostgreSQL pueden proporcionarse mediante `DB_USERNAME` / `DB_PASSWORD`.

La aplicación necesita PostgreSQL y las migraciones de `src/main/resources/db`. No se incluyen credenciales reales ni tokens de producción.
