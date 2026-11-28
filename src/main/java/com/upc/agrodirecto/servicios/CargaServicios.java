package com.upc.agrodirecto.servicios;

import com.upc.agrodirecto.dto.PublicarCargaRequest;
import com.upc.agrodirecto.entidades.Carga;
import com.upc.agrodirecto.entidades.Cultivo;
import com.upc.agrodirecto.entidades.Lote;
import com.upc.agrodirecto.entidades.Usuario;
import com.upc.agrodirecto.repository.CargaRepositorio;
import com.upc.agrodirecto.repository.CultivoRepositorio;
import com.upc.agrodirecto.repository.LoteRepositorio;
import com.upc.agrodirecto.repository.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CargaServicios {
	@Autowired
	private CargaRepositorio cargaRepositorio;
	@Autowired
	private CultivoRepositorio cultivoRepositorio;
	@Autowired
	private UsuarioRepositorio usuarioRepositorio;
	@Autowired
	private LoteRepositorio loteRepositorio;
    @Autowired private ChatServicios chats;

	public Carga publicarCarga(Integer idProductor, PublicarCargaRequest req) {
		Usuario productor = usuarioRepositorio.findById(idProductor).orElseThrow();
		Cultivo cultivo = cultivoRepositorio.findById(req.idCultivo()).orElseThrow();

		if (req.pesoKg() == null || !Double.isFinite(req.pesoKg()) || req.pesoKg() <= 0) {
			throw new IllegalArgumentException("pesoKg debe ser mayor a 0");
		}
		if (req.tarifaPropuesta() == null || !Double.isFinite(req.tarifaPropuesta()) || req.tarifaPropuesta() <= 0) {
			throw new IllegalArgumentException("tarifaPropuesta debe ser mayor a 0");
		}

		Coordenadas.validar(req.latitudAcopio(), req.longitudAcopio(), false);
        Coordenadas.validar(req.latitudDestino(), req.longitudDestino(), false);
        if (req.fechaRecojo().isBefore(java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")))) throw new IllegalArgumentException("La fecha de recojo no puede estar en el pasado");
		Carga carga = new Carga();
		carga.setCodigoCarga("C-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
		carga.setProductor(productor);
		carga.setCultivo(cultivo);
		carga.setPesoKg(req.pesoKg());
		carga.setTipoCarga(req.tipoCarga());
		carga.setDireccionDestino(req.direccionDestino());
		carga.setLatitudAcopio(req.latitudAcopio());
		carga.setLongitudAcopio(req.longitudAcopio());
        carga.setLatitudDestino(req.latitudDestino()); carga.setLongitudDestino(req.longitudDestino());
		carga.setTarifaPropuesta(req.tarifaPropuesta());
		carga.setFechaRecojo(req.fechaRecojo());
		carga.setEsUrgente(req.esUrgente() != null && req.esUrgente());
		carga.setEstadoCarga("publicada");
		carga.setFechaPublicacion(LocalDateTime.now());

		return cargaRepositorio.save(carga);
	}

	@org.springframework.transaction.annotation.Transactional
    public Carga cancelarCarga(Integer idCarga, Integer idProductor) {
		Carga carga = cargaRepositorio.bloquearPorId(idCarga).orElseThrow();
		if (!carga.getProductor().getIdUsuario().equals(idProductor)) {
			throw new org.springframework.security.access.AccessDeniedException("Solo el productor propietario puede cancelar esta carga");
		}
		if (!"publicada".equals(carga.getEstadoCarga()) || carga.getLote() != null) {
			throw new IllegalStateException("Solo se puede cancelar una carga publicada que aún no está asignada a un lote");
		}
		carga.setEstadoCarga("cancelada");
		return cargaRepositorio.save(carga);
	}

	public List<Carga> listarPorEstado(String estado) {
		return cargaRepositorio.listarPorEstado(estado);
	}

	public List<Carga> listarCargasDeRetorno() {
		return cargaRepositorio.listarCargasDeRetornoDisponibles();
	}

	@org.springframework.transaction.annotation.Transactional
	public Carga asignarRetorno(Integer idCarga, Integer idLote, Integer idTransportista) {
        Usuario transportista = usuarioRepositorio.bloquearPorId(idTransportista).orElseThrow();
        Lote lote = loteRepositorio.bloquearPorId(idLote).orElseThrow();
		Carga carga = cargaRepositorio.bloquearPorId(idCarga).orElseThrow();
		if (!"publicada".equals(carga.getEstadoCarga())) {
			throw new IllegalStateException("La carga ya no esta disponible para asignar");
		}
		if (lote.getTransportista() == null || !lote.getTransportista().getIdUsuario().equals(idTransportista)) {
			throw new org.springframework.security.access.AccessDeniedException("El lote no pertenece al transportista autenticado");
		}
        if (!List.of("en_transito", "con_retraso").contains(lote.getEstadoLote())) throw new IllegalStateException("El lote debe estar activo");
        if (!"seco".equals(carga.getTipoCarga()) || carga.getLote() != null) throw new IllegalStateException("La carga no está disponible para retorno");
		Double pesoActual = loteRepositorio.pesoReservado(idTransportista);
		double pesoActualKg = pesoActual == null ? 0.0 : pesoActual;
		double pesoNuevaCargaKg = carga.getPesoKg() == null ? 0.0 : carga.getPesoKg();
		double capacidadKg = (transportista.getCapacidadToneladas() == null ? 0.0 : transportista.getCapacidadToneladas() * 1000.0);
		if (pesoActualKg + pesoNuevaCargaKg > capacidadKg) {
			throw new IllegalArgumentException("La carga supera la capacidad disponible del transportista");
		}
		carga.setLote(lote);
		carga.setEstadoCarga("confirmada_en_lote");
        Carga guardada=cargaRepositorio.saveAndFlush(carga);
        // Cargar colección después de guardar la relación.
        chats.crearParaLote(lote);
        return guardada;
	}
}
