#!/usr/bin/env python3
"""Prueba de humo de una cuenta (login y GET); no inserta datos ni modifica pagos.
Ejecutar desde EC2: python3 scripts/probar_integracion.py http://localhost:8080
El login actualiza ultimo_acceso. No imprime token/contraseña.
"""
import sys, json, getpass, math
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError
BASE=(sys.argv[1] if len(sys.argv)>1 else 'http://localhost:8080').rstrip('/')+'/api/v1'
token=None; fails=[]
def request(path, data=None):
    headers={'Accept':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    if data is not None: headers['Content-Type']='application/json'
    req=Request(BASE+path, data=json.dumps(data).encode() if data is not None else None,headers=headers)
    with urlopen(req, timeout=20) as res: return json.load(res)
def test(path, predicate=lambda x: True):
    try:
        obj=request(path); assert predicate(obj), 'estructura o cálculo inesperado'; print('OK GET',path); return obj
    except (HTTPError,URLError,AssertionError,KeyError,ValueError,TypeError) as e:
        print('ERROR GET',path,':',e); fails.append(path); return None
try:
    login=request('/auth/login', {'correo':input('Correo de prueba: ').strip(),'contrasena':getpass.getpass('Contraseña: ')})
    token=login['token']; uid=login['idUsuario']; rol=login['rol'].lower(); print('OK login',rol)
except Exception as e:
    print('ERROR login:',e);sys.exit(1)
if rol=='administrador':
    r=test('/reportes/transacciones/resumen',lambda x: all(k in x for k in ['mes','totalTransado','comisionAcumulada']))
    if r:
        ls=test('/transacciones?mes='+r['mes'],lambda x:isinstance(x,list))
        if ls is not None:
            ok=math.isclose(sum(l.get('fleteTotal') or 0 for l in ls),r['totalTransado'],abs_tol=.01) and math.isclose(sum(l.get('montoComision') or 0 for l in ls),r['comisionAcumulada'],abs_tol=.01)
            print('OK totales del mes' if ok else 'ERROR sumas del reporte', 'lotes:',len(ls))
            if not ok:fails.append('sumas reporte')
    test('/usuarios',lambda x:isinstance(x,list));test('/cargas',lambda x:isinstance(x,list));test('/chats/solicitudes',lambda x:isinstance(x,list))
    test(f'/usuarios/{uid}/administrador')
elif rol=='productor':
    test(f'/productores/{uid}/cargas',lambda x:isinstance(x,list));test(f'/productores/{uid}/saldo',lambda x:'saldoDisponible' in x)
    test(f'/reportes/ahorro-productor/{uid}');ls=test(f'/productores/{uid}/lotes-consolidacion',lambda x:isinstance(x,list))
    test(f'/productores/{uid}/lotes',lambda x:isinstance(x,list))
    if ls:
        lid=ls[0]['idLote'];test(f'/lotes/{lid}/seguimiento');test(f'/lotes/{lid}/financiero',lambda x: 'montoNeto' in x);test(f'/calificaciones/lote/{lid}',lambda x:isinstance(x,list))
elif rol=='transportista':
    ls=test(f'/transportistas/{uid}/lotes?estado=activos',lambda x:isinstance(x,list));test('/cargas/retorno',lambda x:isinstance(x,list));test(f'/usuarios/{uid}/transportista');test(f'/reportes/reputacion-transportista/{uid}')
    if ls:
        lid=ls[0]['idLote'];test(f'/lotes/{lid}/seguimiento');test(f'/lotes/{lid}/gastos',lambda x:isinstance(x.get('gastos'),list) and 'totalGastos' in x)
if rol in ('productor','transportista'):
    test(f'/usuarios/{uid}/suscripcion-premium',lambda x:'esPremium' in x and 'premiumHasta' in x)
    test(f'/notificaciones/usuario/{uid}',lambda x:'totalNoLeidas' in x)
    chats=test('/chats/solicitudes',lambda x:isinstance(x,list))
    if chats is not None:
        sol=next((s for s in chats if s['estadoSolicitud']=='aprobado'),None)
        if sol:test(f'/chats/solicitudes/{sol["idSolicitud"]}/mensajes',lambda x:isinstance(x,list) and all('usuarioEmisor' in m for m in x))
        else: print('OMITIDO mensajes: cuenta sin chat aprobado')
    test('/precios-mercado',lambda x:isinstance(x,list))
print('Resultado:',len(fails),'fallos; algunos detalles se omiten si no hay datos. No prueba acciones de escritura ni navegador.')
sys.exit(bool(fails))
