package com.upc.agrodirecto.servicios;
import com.upc.agrodirecto.dto.ConsolidarLoteRequest;
import com.upc.agrodirecto.entidades.*;
import com.upc.agrodirecto.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import java.util.*;

@Service
public class ConsolidacionServicios {
 private final CargaRepositorio cargas; private final LoteRepositorio lotes; private final UsuarioRepositorio usuarios;
 private final ChatServicios chats; private final NotificacionRepositorio notificaciones;
 public ConsolidacionServicios(CargaRepositorio cargas,LoteRepositorio lotes,UsuarioRepositorio usuarios,ChatServicios chats,NotificacionRepositorio notificaciones){this.cargas=cargas;this.lotes=lotes;this.usuarios=usuarios;this.chats=chats;this.notificaciones=notificaciones;}
 @Transactional
 public Lote consolidar(ConsolidarLoteRequest req,Usuario admin){
  if(admin.getRol().getIdRol()!=3) throw new AccessDeniedException("Solo el administrador consolida cargas");
  if(req.fleteTotal()==null||!Double.isFinite(req.fleteTotal())||req.fleteTotal()<=0) throw new IllegalArgumentException("Flete inválido");
  Usuario t=usuarios.bloquearPorId(req.idTransportista()).orElseThrow();
  if(t.getRol().getIdRol()!=2||!"verificado".equals(t.getEstadoCuenta())||t.getCapacidadToneladas()==null||!Double.isFinite(t.getCapacidadToneladas())||t.getCapacidadToneladas()<=0) throw new IllegalArgumentException("Selecciona un transportista verificado");
  List<Integer> ids=req.idsCargas().stream().distinct().sorted().toList();
  if(ids.size()!=req.idsCargas().size()) throw new IllegalArgumentException("Cargas repetidas");
  List<Carga> grupo=ids.stream().map(id->cargas.bloquearPorId(id).orElseThrow()).toList();
  Carga base=grupo.get(0); String destino=base.getDireccionDestino().trim();
  if(destino.length()>100) throw new IllegalArgumentException("El destino debe tener como máximo 100 caracteres");
  if(base.getFechaRecojo().isBefore(LocalDate.now(ZoneId.of("America/Lima")))) throw new IllegalArgumentException("Fecha de recojo vencida");
  double peso=0;
  for(Carga c:grupo){
   if(c.getLote()!=null||!"publicada".equals(c.getEstadoCarga())) throw new IllegalStateException("Una carga ya no está disponible");
   if(!"verificado".equals(c.getProductor().getEstadoCuenta())) throw new IllegalStateException("Hay un productor no verificado");
   Coordenadas.validar(c.getLatitudAcopio(),c.getLongitudAcopio(),false); Coordenadas.validar(c.getLatitudDestino(),c.getLongitudDestino(),false);
   if(!destino.equalsIgnoreCase(c.getDireccionDestino().trim()) || distancia(c.getLatitudDestino(),c.getLongitudDestino(),base.getLatitudDestino(),base.getLongitudDestino())>0.5 || !base.getFechaRecojo().equals(c.getFechaRecojo()) || !base.getTipoCarga().equals(c.getTipoCarga())) throw new IllegalArgumentException("Las cargas deben tener el mismo destino (hasta 500 m de diferencia), fecha de recojo y tipo");
   if(!Double.isFinite(c.getPesoKg())||c.getPesoKg()<=0)throw new IllegalArgumentException("Peso inválido"); peso+=c.getPesoKg();
  }
  if(peso+lotes.pesoReservado(t.getIdUsuario())>t.getCapacidadToneladas()*1000) throw new IllegalStateException("Capacidad disponible insuficiente");
  Lote l=new Lote(); l.setCodigoLote("L-"+UUID.randomUUID().toString().substring(0,12).toUpperCase()); l.setTransportista(t); l.setEstadoLote("formandose"); l.setFechaCreacion(LocalDateTime.now()); l.setOrigen(base.getProductor().getDistrito()); l.setDestino(destino); l.setLatitudOrigen(base.getLatitudAcopio());l.setLongitudOrigen(base.getLongitudAcopio());l.setLatitudDestino(base.getLatitudDestino());l.setLongitudDestino(base.getLongitudDestino()); l.setFechaSalida(base.getFechaRecojo().atStartOfDay());l.setFleteTotal(req.fleteTotal());l.setPorcentajeComision(5.0);l.setMontoComision(Math.round(req.fleteTotal()*5)/100.0);l.setMontoNeto(req.fleteTotal()-l.getMontoComision());l.setEstadoPago("pendiente");
  lotes.save(l); for(Carga c:grupo){c.setLote(l);c.setEstadoCarga("en_revision");cargas.save(c);} l.setCargas(new ArrayList<>(grupo));
  chats.crearParaLote(l); avisar(t,"Nuevo lote asignado",l.getCodigoLote()); for(Usuario p:grupo.stream().map(Carga::getProductor).collect(java.util.stream.Collectors.toMap(Usuario::getIdUsuario,u->u,(a,b)->a)).values())avisar(p,"Confirma tu flete",l.getCodigoLote()); return l;
 }
 @Transactional public Lote confirmar(Integer id,Usuario p){
  Lote l=lotes.bloquearPorId(id).orElseThrow(); if(p.getRol().getIdRol()!=1||l.getCargas().stream().noneMatch(c->c.getProductor().getIdUsuario().equals(p.getIdUsuario())))throw new AccessDeniedException("No perteneces al lote");
  if(!"formandose".equals(l.getEstadoLote()))throw new IllegalStateException("El lote ya no espera confirmación");
  for(Carga c:l.getCargas())if(c.getProductor().getIdUsuario().equals(p.getIdUsuario())&&"en_revision".equals(c.getEstadoCarga())){c.setEstadoCarga("confirmada_en_lote");cargas.save(c);}
  if(l.getCargas().stream().allMatch(c->"confirmada_en_lote".equals(c.getEstadoCarga()))){l.setEstadoLote("confirmado");avisar(l.getTransportista(),"Lote listo para iniciar",l.getCodigoLote());}return lotes.save(l);
 }
 @Transactional public Lote iniciar(Integer id,Usuario t){
  Lote l=lotes.bloquearPorId(id).orElseThrow();if(t.getRol().getIdRol()!=2||l.getTransportista()==null||!l.getTransportista().getIdUsuario().equals(t.getIdUsuario()))throw new AccessDeniedException("No eres el transportista asignado");
  if(!"confirmado".equals(l.getEstadoLote()))throw new IllegalStateException("El lote debe estar confirmado");
  Coordenadas.validar(l.getLatitudOrigen(),l.getLongitudOrigen(),false);Coordenadas.validar(l.getLatitudDestino(),l.getLongitudDestino(),false);
  if(l.getCargas().isEmpty()||l.getCargas().stream().anyMatch(c->!"confirmada_en_lote".equals(c.getEstadoCarga())))throw new IllegalStateException("Faltan cargas confirmadas");
  l.setEstadoLote("en_transito");l.setFechaSalida(LocalDateTime.now());for(Carga c:l.getCargas()){c.setEstadoCarga("en_transito");cargas.save(c);}chats.crearParaLote(l);return lotes.save(l);
 }
 private void avisar(Usuario u,String titulo,String msg){Notificacion n=new Notificacion();n.setUsuario(u);n.setTitulo(titulo);n.setMensaje(msg);n.setLeida(false);n.setFechaCreacion(LocalDateTime.now());notificaciones.save(n);}
 static double distancia(double a,double b,double c,double d){double x=Math.toRadians(c-a),y=Math.toRadians(d-b);double h=Math.pow(Math.sin(x/2),2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(c))*Math.pow(Math.sin(y/2),2);return 6371*2*Math.asin(Math.sqrt(Math.min(1,h)));}
}
