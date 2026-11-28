package com.upc.agrodirecto.servicios;

import com.upc.agrodirecto.dto.EstadoSolicitudRequest;
import com.upc.agrodirecto.dto.MensajeChatRequest;
import com.upc.agrodirecto.entidades.MensajeChat;
import com.upc.agrodirecto.entidades.SolicitudChat;
import com.upc.agrodirecto.entidades.Usuario;
import com.upc.agrodirecto.repository.MensajeChatRepositorio;
import com.upc.agrodirecto.repository.SolicitudChatRepositorio;
import com.upc.agrodirecto.repository.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatServicios {
	@Autowired
	private SolicitudChatRepositorio solicitudChatRepositorio;
	@Autowired
	private MensajeChatRepositorio mensajeChatRepositorio;
	@Autowired
	private UsuarioRepositorio usuarioRepositorio;

	@Autowired private com.upc.agrodirecto.repository.LoteRepositorio loteRepositorio;
    @org.springframework.transaction.annotation.Transactional
    public void crearParaLote(com.upc.agrodirecto.entidades.Lote l) {
        if(l.getTransportista()==null)return;
        for(var c:l.getCargas()) {
            if("cancelada".equals(c.getEstadoCarga())) continue;
            Usuario p=c.getProductor();
            if(!solicitudChatRepositorio.existsByProductor_IdUsuarioAndTransportista_IdUsuarioAndLote_IdLote(p.getIdUsuario(),l.getTransportista().getIdUsuario(),l.getIdLote())) {
                SolicitudChat s=new SolicitudChat();s.setProductor(p);s.setTransportista(l.getTransportista());s.setLote(l);s.setEstadoSolicitud("pendiente");s.setFechaSolicitud(LocalDateTime.now());solicitudChatRepositorio.saveAndFlush(s);
            }
        }
    }
    @org.springframework.transaction.annotation.Transactional
    public List<SolicitudChat> solicitar(Integer id,Usuario u) {
        var l=loteRepositorio.bloquearPorId(id).orElseThrow();
        boolean participa=u.getRol().getIdRol()==3 || (l.getTransportista()!=null && l.getTransportista().getIdUsuario().equals(u.getIdUsuario())) || l.getCargas().stream().anyMatch(c->c.getProductor().getIdUsuario().equals(u.getIdUsuario()));
        if(!participa)throw new org.springframework.security.access.AccessDeniedException("No perteneces al lote");
        if(l.getTransportista()==null || !List.of("formandose","confirmado","en_transito","con_retraso").contains(l.getEstadoLote()))throw new IllegalStateException("Lote sin transportista o finalizado");
        crearParaLote(l);return listarSolicitudes(null,u).stream().filter(s->s.getLote().getIdLote().equals(id)).toList();
    }
    public List<SolicitudChat> listarSolicitudes(String estado, Usuario usuario) {
        return solicitudChatRepositorio.listarPorEstado(estado).stream()
            .filter(s -> usuario.getRol().getIdRol() == 3 || s.getProductor().getIdUsuario().equals(usuario.getIdUsuario())
                    || s.getTransportista().getIdUsuario().equals(usuario.getIdUsuario())).toList();
    }

    public List<MensajeChat> listarMensajes(Integer idSolicitud, Integer idUsuario) {
        SolicitudChat solicitud = solicitudChatRepositorio.findById(idSolicitud).orElseThrow();
        boolean participante = solicitud.getProductor().getIdUsuario().equals(idUsuario)
                || solicitud.getTransportista().getIdUsuario().equals(idUsuario);
        if (!participante || !"aprobado".equals(solicitud.getEstadoSolicitud()))
            throw new org.springframework.security.access.AccessDeniedException("El chat debe estar aprobado y pertenecer al usuario");
        return mensajeChatRepositorio.findBySolicitud_IdSolicitudOrderByFechaEnvioAscIdMensajeAsc(idSolicitud);
    }

	@org.springframework.transaction.annotation.Transactional
    public SolicitudChat moderarSolicitud(Integer idSolicitud, EstadoSolicitudRequest req, Integer idAdministradorAutenticado) {
		SolicitudChat solicitud = solicitudChatRepositorio.bloquear(idSolicitud).orElseThrow();
		if (!"pendiente".equals(solicitud.getEstadoSolicitud())) {
			throw new IllegalStateException("La solicitud ya fue moderada");
		}
		Usuario administrador = usuarioRepositorio.findById(idAdministradorAutenticado).orElseThrow();
		if (administrador.getRol() == null || administrador.getRol().getIdRol() != 3) {
			throw new org.springframework.security.access.AccessDeniedException("Solo un administrador puede moderar solicitudes");
		}
		if (!List.of("aprobado", "rechazado").contains(req.estadoSolicitud())) throw new IllegalArgumentException("Estado de solicitud inválido");
		solicitud.setEstadoSolicitud(req.estadoSolicitud());
		solicitud.setAdministrador(administrador);
		return solicitudChatRepositorio.save(solicitud);
	}

	public MensajeChat enviarMensaje(Integer idSolicitud, MensajeChatRequest req, Integer idEmisorAutenticado) {
		SolicitudChat solicitud = solicitudChatRepositorio.findById(idSolicitud).orElseThrow();
		if (!"aprobado".equals(solicitud.getEstadoSolicitud())) {
			throw new org.springframework.security.access.AccessDeniedException("El chat no está aprobado todavía");
		}
		Usuario emisor = usuarioRepositorio.findById(idEmisorAutenticado).orElseThrow();
		boolean participante = solicitud.getProductor().getIdUsuario().equals(idEmisorAutenticado)
				|| solicitud.getTransportista().getIdUsuario().equals(idEmisorAutenticado);
		if (!participante) throw new org.springframework.security.access.AccessDeniedException("Solo las partes del chat pueden enviar mensajes");
		if (req.contenido() == null || req.contenido().isBlank() || req.contenido().length() > 500) {
			throw new IllegalArgumentException("El contenido debe tener entre 1 y 500 caracteres");
		}

		MensajeChat mensaje = new MensajeChat();
		mensaje.setSolicitud(solicitud);
		mensaje.setUsuarioEmisor(emisor);
		mensaje.setContenido(req.contenido());
		mensaje.setFechaEnvio(LocalDateTime.now());
		return mensajeChatRepositorio.save(mensaje);
	}
}
