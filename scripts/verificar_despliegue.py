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
    ('get', '/api/v1/calificaciones/lote/{idLote}'): "Contrato Angular",
    ('get', '/api/v1/cargas'): "Contrato Angular",
    ('get', '/api/v1/cargas/retorno'): "Contrato Angular",
    ('get', '/api/v1/chats/solicitudes'): "Contrato Angular",
    ('get', '/api/v1/chats/solicitudes/{idSolicitud}/mensajes'): "Contrato Angular",
    ('get', '/api/v1/cultivos'): "Contrato Angular",
    ('get', '/api/v1/lotes/{idLote}'): "Contrato Angular",
    ('get', '/api/v1/lotes/{idLote}/financiero'): "Contrato Angular",
    ('get', '/api/v1/lotes/{idLote}/gastos'): "Contrato Angular",
    ('get', '/api/v1/lotes/{idLote}/seguimiento'): "Contrato Angular",
    ('get', '/api/v1/notificaciones/usuario/{idUsuario}'): "Contrato Angular",
    ('get', '/api/v1/precios-mercado'): "Contrato Angular",
    ('get', '/api/v1/productores/{idProductor}/cargas'): "Contrato Angular",
    ('get', '/api/v1/productores/{idProductor}/lotes'): "Contrato Angular",
    ('get', '/api/v1/productores/{idProductor}/lotes-consolidacion'): "Contrato Angular",
    ('get', '/api/v1/productores/{idProductor}/saldo'): "Contrato Angular",
    ('get', '/api/v1/reportes/ahorro-productor/{idProductor}'): "Contrato Angular",
    ('get', '/api/v1/reportes/reputacion-transportista/{idTransportista}'): "Contrato Angular",
    ('get', '/api/v1/reportes/transacciones/resumen'): "Contrato Angular",
    ('get', '/api/v1/transacciones'): "Contrato Angular",
    ('get', '/api/v1/transportistas/{idTransportista}/lotes'): "Contrato Angular",
    ('get', '/api/v1/usuarios'): "Contrato Angular",
    ('get', '/api/v1/usuarios/{idUsuario}/administrador'): "Contrato Angular",
    ('get', '/api/v1/usuarios/{idUsuario}/suscripcion-premium'): "Contrato Angular",
    ('get', '/api/v1/usuarios/{idUsuario}/transportista'): "Contrato Angular",
    ('patch', '/api/v1/cargas/{idCarga}/asignar-retorno'): "Contrato Angular",
    ('patch', '/api/v1/cargas/{idCarga}/cancelar'): "Contrato Angular",
    ('patch', '/api/v1/lotes/{idLote}/confirmar-entrega'): "Contrato Angular",
    ('patch', '/api/v1/lotes/{idLote}/reportar-retraso'): "Contrato Angular",
    ('patch', '/api/v1/lotes/{idLote}/ubicacion'): "Contrato Angular",
    ('patch', '/api/v1/notificaciones/{idNotificacion}/leer'): "Contrato Angular",
    ('patch', '/api/v1/usuarios/{idUsuario}/estado'): "Contrato Angular",
    ('post', '/api/v1/auth/login'): "Contrato Angular",
    ('post', '/api/v1/auth/registro-productor'): "Contrato Angular",
    ('post', '/api/v1/auth/registro-transportista'): "Contrato Angular",
    ('post', '/api/v1/calificaciones'): "Contrato Angular",
    ('post', '/api/v1/cargas'): "Contrato Angular",
    ('post', '/api/v1/chats/solicitudes/{idSolicitud}/mensajes'): "Contrato Angular",
    ('post', '/api/v1/lotes/{idLote}/gastos'): "Contrato Angular",
    ('post', '/api/v1/usuarios/{idUsuario}/suscripcion-premium'): "Contrato Angular",
    ('put', '/api/v1/chats/solicitudes/{idSolicitud}/estado'): "Contrato Angular",
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
