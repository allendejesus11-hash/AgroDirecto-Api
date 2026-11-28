package com.upc.agrodirecto.controller;

import com.upc.agrodirecto.dto.EstadoSolicitudRequest;
import com.upc.agrodirecto.dto.MensajeChatRequest;
import com.upc.agrodirecto.entidades.MensajeChat;
import com.upc.agrodirecto.entidades.SolicitudChat;
import com.upc.agrodirecto.servicios.ChatServicios;
import com.upc.agrodirecto.security.service.AuthenticatedUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chats/solicitudes")
public class ChatController {
	@Autowired
	private ChatServicios chatServicios;
	@Autowired
	private AuthenticatedUserService authenticatedUserService;

	@PostMapping("/lote/{idLote}")
    public List<ChatVista> solicitar(@PathVariable Integer idLote){return chatServicios.solicitar(idLote,authenticatedUserService.getCurrentUser()).stream().map(ChatController::vista).toList();}
    @GetMapping
	public List<ChatVista> listar(@RequestParam(required = false) String estado) {
		return chatServicios.listarSolicitudes(estado, authenticatedUserService.getCurrentUser()).stream().map(ChatController::vista).toList();
	}

	@PutMapping("/{idSolicitud}/estado")
	public ChatVista moderar(@PathVariable Integer idSolicitud, @RequestBody EstadoSolicitudRequest req) {
		return vista(chatServicios.moderarSolicitud(idSolicitud, req, authenticatedUserService.getCurrentUser().getIdUsuario()));
	}

    @GetMapping("/{idSolicitud}/mensajes")
    public List<MensajeVista> mensajes(@PathVariable Integer idSolicitud) {
        return chatServicios.listarMensajes(idSolicitud, authenticatedUserService.getCurrentUser().getIdUsuario()).stream().map(ChatController::vistaMensaje).toList();
    }
	@PostMapping("/{idSolicitud}/mensajes")
	public ResponseEntity<MensajeVista> enviarMensaje(@PathVariable Integer idSolicitud, @RequestBody MensajeChatRequest req) {
		MensajeChat mensaje = chatServicios.enviarMensaje(idSolicitud, req, authenticatedUserService.getCurrentUser().getIdUsuario());
		return ResponseEntity.status(HttpStatus.CREATED).body(vistaMensaje(mensaje));
	}
    public record PersonaVista(Integer idUsuario,String nombreCompleto) {}
    public record LoteVista(Integer idLote,String codigoLote,String destino) {}
    public record ChatVista(Integer idSolicitud,PersonaVista productor,PersonaVista transportista,LoteVista lote,PersonaVista administrador,String estadoSolicitud,java.time.LocalDateTime fechaSolicitud) {}
    public record MensajeVista(Integer idMensaje,PersonaVista usuarioEmisor,String contenido,java.time.LocalDateTime fechaEnvio) {}
    private static PersonaVista persona(com.upc.agrodirecto.entidades.Usuario u){return u==null?null:new PersonaVista(u.getIdUsuario(),u.getNombreCompleto());}
    private static ChatVista vista(SolicitudChat s){var l=s.getLote();return new ChatVista(s.getIdSolicitud(),persona(s.getProductor()),persona(s.getTransportista()),new LoteVista(l.getIdLote(),l.getCodigoLote(),l.getDestino()),persona(s.getAdministrador()),s.getEstadoSolicitud(),s.getFechaSolicitud());}
    private static MensajeVista vistaMensaje(MensajeChat m){return new MensajeVista(m.getIdMensaje(),persona(m.getUsuarioEmisor()),m.getContenido(),m.getFechaEnvio());}
}
