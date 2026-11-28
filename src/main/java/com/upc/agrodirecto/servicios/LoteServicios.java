package com.upc.agrodirecto.servicios;

import com.upc.agrodirecto.dto.ConfirmarEntregaRequest;
import com.upc.agrodirecto.dto.UbicacionLoteRequest;
import com.upc.agrodirecto.dto.PuntosRutaRequest;
import com.upc.agrodirecto.entidades.Usuario;
import com.upc.agrodirecto.entidades.Notificacion;
import com.upc.agrodirecto.repository.NotificacionRepositorio;
import com.upc.agrodirecto.dto.ReportarRetrasoRequest;
import com.upc.agrodirecto.entidades.Lote;
import com.upc.agrodirecto.repository.LoteRepositorio;
import com.upc.agrodirecto.repository.CargaRepositorio;
import com.upc.agrodirecto.entidades.Carga;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoteServicios {
	@Autowired
	private LoteRepositorio loteRepositorio;
	@Autowired
	private CargaRepositorio cargaRepositorio;
	@Autowired private NotificacionRepositorio notificacionRepositorio;

	public List<Lote> historialTransportista(Integer idTransportista, String filtro) {
		return loteRepositorio.listarPorTransportista(idTransportista, filtro);
	}

	public Lote detalleConCargas(Integer idLote) {
		return loteRepositorio.buscarDetalleConCargas(idLote).orElseThrow();
	}

	@Transactional
	public Lote confirmarEntrega(Integer idLote, ConfirmarEntregaRequest req, Integer idTransportistaAutenticado) {
		Lote lote = loteRepositorio.bloquearPorId(idLote).orElseThrow();
		if (lote.getTransportista() == null || !lote.getTransportista().getIdUsuario().equals(idTransportistaAutenticado)) {
			throw new org.springframework.security.access.AccessDeniedException("El lote no pertenece al transportista autenticado");
		}
		if (req.codigoValidacion() == null || !req.codigoValidacion().trim().equalsIgnoreCase(lote.getCodigoLote())) {
			throw new IllegalArgumentException("El código QR/manual no coincide con el código del lote");
		}
		if (!List.of("en_transito", "con_retraso").contains(lote.getEstadoLote())) {
			throw new IllegalStateException("El lote no esta en transito, no se puede confirmar entrega");
		}
		lote.setEstadoLote("entregado");
		lote.setFechaLlegada(req.fechaLlegada() != null ? req.fechaLlegada() : LocalDateTime.now());
		Lote guardado = loteRepositorio.save(lote);
		for (Carga carga : lote.getCargas() == null ? List.<Carga>of() : lote.getCargas()) {
			carga.setEstadoCarga("entregada");
			cargaRepositorio.save(carga);
		}
		return guardado;
	}

	@Transactional
	public Lote reportarRetraso(Integer idLote, ReportarRetrasoRequest req, Integer idTransportistaAutenticado) {
		Lote lote = loteRepositorio.bloquearPorId(idLote).orElseThrow();
		if (lote.getTransportista() == null || !lote.getTransportista().getIdUsuario().equals(idTransportistaAutenticado)) {
			throw new org.springframework.security.access.AccessDeniedException("El lote no pertenece al transportista autenticado");
		}
		if (!"en_transito".equals(lote.getEstadoLote())) {
			throw new IllegalStateException("El lote no esta en transito");
		}
        lote.setEstadoLote("con_retraso");
        lote.setMotivoRetraso(req.motivo());
        // Reportar un retraso no equivale a registrar una nueva posición GPS.
        lote.getCargas().stream().map(Carga::getProductor).filter(java.util.Objects::nonNull)
                .map(Usuario::getIdUsuario).distinct().forEach(id -> {
                    Usuario productor = lote.getCargas().stream().map(Carga::getProductor)
                            .filter(u -> u != null && u.getIdUsuario().equals(id)).findFirst().orElseThrow();
                    Notificacion n = new Notificacion(); n.setUsuario(productor);
                    n.setTitulo("Retraso del lote " + lote.getCodigoLote()); n.setMensaje(req.motivo());
                    n.setLeida(false); n.setFechaCreacion(LocalDateTime.now()); notificacionRepositorio.save(n);
                });
        return loteRepositorio.save(lote);
	}

    public Lote seguimiento(Integer idLote, Usuario usuario) {
        Lote lote = loteRepositorio.findById(idLote).orElseThrow();
        int rol = usuario.getRol().getIdRol();
        boolean permitido = rol == 3 || (rol == 2 && lote.getTransportista() != null && lote.getTransportista().getIdUsuario().equals(usuario.getIdUsuario()))
                || (rol == 1 && cargaRepositorio.existsByLote_IdLoteAndProductor_IdUsuario(idLote, usuario.getIdUsuario()));
        if (!permitido) throw new org.springframework.security.access.AccessDeniedException("No tienes acceso a este lote");
        return lote;
    }

    @Transactional
    public Lote guardarPuntosRuta(Integer idLote, PuntosRutaRequest req, Usuario usuario) {
        Lote lote = loteRepositorio.bloquearPorId(idLote).orElseThrow();
        if (usuario.getRol().getIdRol() != 2 || lote.getTransportista() == null
                || !lote.getTransportista().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new org.springframework.security.access.AccessDeniedException("Solo el transportista asignado puede confirmar los puntos de ruta");
        }
        if (!List.of("confirmado", "en_transito", "con_retraso").contains(lote.getEstadoLote()))
            throw new IllegalStateException("El lote debe estar confirmado o activo");
        Coordenadas.validar(req.latitudOrigen(), req.longitudOrigen(), false);
        Coordenadas.validar(req.latitudDestino(), req.longitudDestino(), false);
        boolean cambioDestino = !java.util.Objects.equals(lote.getLatitudDestino(), req.latitudDestino())
                || !java.util.Objects.equals(lote.getLongitudDestino(), req.longitudDestino());
        lote.setLatitudOrigen(req.latitudOrigen());
        lote.setLongitudOrigen(req.longitudOrigen());
        lote.setLatitudDestino(req.latitudDestino());
        lote.setLongitudDestino(req.longitudDestino());
        if (cambioDestino) lote.setEta(null); // La estimación anterior ya no corresponde al destino.
        return loteRepositorio.save(lote);
    }

    @Transactional
    public Lote actualizarUbicacion(Integer idLote, UbicacionLoteRequest req, Usuario usuario) {
        Lote lote = loteRepositorio.bloquearPorId(idLote).orElseThrow();
        seguimiento(idLote, usuario);
        if (usuario.getRol().getIdRol() != 2 || !List.of("en_transito", "con_retraso").contains(lote.getEstadoLote()))
            throw new org.springframework.security.access.AccessDeniedException("Solo el transportista de un viaje activo puede actualizar la ubicación");
        Coordenadas.validar(req.latitud(), req.longitud(), false);
        lote.setLatitud(req.latitud()); lote.setLongitud(req.longitud());
        lote.setEta(req.eta());
        lote.setFechaActualizacion(LocalDateTime.now(java.time.ZoneOffset.UTC));
        return loteRepositorio.save(lote);
    }

	public List<Lote> historialProductor(Integer idProductor) {
		return loteRepositorio.listarLotesEntregadosDeProductor(idProductor);
	}

    @Transactional(readOnly = true)
    public Double saldoProductor(Integer idProductor) {
        return redondear(loteRepositorio.listarLotesPagadosDeProductor(idProductor).stream()
                .mapToDouble(l -> netoProductor(l, idProductor)).sum());
    }

    // El modelo almacena importes a nivel de lote. Se estima la parte del productor por su peso,
    // igual que el cálculo de flete consolidado. No representa una cuenta bancaria ni un pago real.
    public static double montoProporcional(Lote l, Integer idProductor, Double importe) {
        if (importe == null || l.getCargas() == null) return 0.0;
        List<Carga> cargas = l.getCargas().stream().filter(c -> !"cancelada".equals(c.getEstadoCarga()) && c.getPesoKg() != null).toList();
        double total = cargas.stream().mapToDouble(Carga::getPesoKg).sum();
        double propio = cargas.stream().filter(c -> c.getProductor().getIdUsuario().equals(idProductor)).mapToDouble(Carga::getPesoKg).sum();
        return total > 0 ? redondear(importe * propio / total) : 0.0;
    }
    public static double netoProductor(Lote l, Integer idProductor) {
        if (l.getMontoNeto() == null) return 0.0;
        return redondear(montoProporcional(l,idProductor,l.getFleteTotal()) - montoProporcional(l,idProductor,l.getMontoComision()));
    }
    private static double redondear(double v) { return java.math.BigDecimal.valueOf(v).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue(); }

    @Transactional(readOnly = true)
    public java.util.Map<String,Object> financiero(Integer idLote, Usuario usuario) {
        Lote l = seguimiento(idLote, usuario);
        boolean productor = usuario.getRol().getIdRol() == 1;
        java.util.Map<String,Object> r = new java.util.LinkedHashMap<>();
        r.put("idLote", l.getIdLote()); r.put("codigoLote", l.getCodigoLote());
        r.put("fleteTotal", productor ? montoProporcional(l, usuario.getIdUsuario(), l.getFleteTotal()) : l.getFleteTotal());
        r.put("porcentajeComision", l.getPorcentajeComision());
        r.put("montoComision", productor ? montoProporcional(l, usuario.getIdUsuario(), l.getMontoComision()) : l.getMontoComision());
        r.put("montoNeto", productor ? netoProductor(l, usuario.getIdUsuario()) : l.getMontoNeto());
        r.put("estadoPago", l.getEstadoPago());
        return r;
    }
}
