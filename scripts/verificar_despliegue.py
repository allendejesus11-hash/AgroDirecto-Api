#!/usr/bin/env python3
"""Verifica rutas publicadas por una instancia AWS de AgroDirecto mediante OpenAPI.
Solo lee /v3/api-docs. No necesita credenciales ni modifica datos.
Uso: python3 scripts/verificar_despliegue.py http://localhost:8080
"""
import json
import sys
from urllib.request import urlopen
from urllib.error import URLError, HTTPError

BASE = sys.argv[1].rstrip('/') if len(sys.argv) > 1 else 'http://localhost:8080'
RUTAS_ESENCIALES = {
    ('post', '/api/v1/auth/login'): 'Inicio de sesion',
    ('post', '/api/v1/auth/registro-productor'): 'Registro productor',
    ('post', '/api/v1/auth/registro-transportista'): 'Registro transportista',
    ('get', '/api/v1/productores/{idProductor}/cargas'): 'Cargas del productor (error 404)',
    ('get', '/api/v1/productores/{idProductor}/lotes-consolidacion'): 'Consolidaciones del productor',
    ('get', '/api/v1/productores/{idProductor}/saldo'): 'Saldo del productor',
    ('get', '/api/v1/productores/{idProductor}/lotes'): 'Lotes del productor',
    ('get', '/api/v1/reportes/ahorro-productor/{idProductor}'): 'Ahorro del productor',
    ('get', '/api/v1/reportes/transacciones/resumen'): 'Reporte del administrador (error 500)',
    ('get', '/api/v1/usuarios'): 'Usuarios del administrador',
    ('get', '/api/v1/cargas'): 'Listado de cargas admin',
    ('post', '/api/v1/cargas'): 'Publicar carga',
    ('get', '/api/v1/transportistas/{idTransportista}/lotes'): 'Lotes del transportista',
    ('get', '/api/v1/cultivos'): 'Cultivos',
    ('get', '/api/v1/precios-mercado'): 'Precios',
    ('get', '/api/v1/notificaciones/usuario/{idUsuario}'): 'Notificaciones',
}

try:
    with urlopen(BASE + '/v3/api-docs', timeout=15) as r:
        if r.status != 200:
            raise RuntimeError(f'HTTP {r.status} leyendo OpenAPI')
        doc = json.load(r)
except (URLError, HTTPError, OSError, ValueError, RuntimeError) as ex:
    print(f'ERROR: No se pudo consultar {BASE}/v3/api-docs: {ex}')
    sys.exit(2)

paths = doc.get('paths', {})
fallos = []
for (method, path), desc in RUTAS_ESENCIALES.items():
    exists = method in paths.get(path, {})
    print(('OK      ' if exists else 'FALTA   ') + method.upper() + ' ' + path + '  - ' + desc)
    if not exists: fallos.append((method, path))

if fallos:
    print(f'\nFALTAN {len(fallos)} rutas. Reconstruye la API desde este proyecto: sudo docker compose up -d --build --force-recreate app')
    sys.exit(1)
print('\nLas rutas principales estan publicadas. Nota: esto NO prueba permisos JWT ni datos.')
