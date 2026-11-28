package com.upc.agrodirecto.controller;

import com.upc.agrodirecto.dto.ConfirmarEntregaRequest;
import com.upc.agrodirecto.dto.UbicacionLoteRequest;
import com.upc.agrodirecto.dto.PuntosRutaRequest;
import com.upc.agrodirecto.dto.ReportarRetrasoRequest;
import com.upc.agrodirecto.entidades.Lote;
import com.upc.agrodirecto.servicios.LoteServicios;
import com.upc.agrodirecto.security.service.AuthenticatedUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/lotes")
public class LoteController {
	@Autowired
	private LoteServicios loteServicios;
	@Autowired
	private AuthenticatedUserService authenticatedUserService;

	@GetMapping("/{idLote}")
	public Lote detalle(@PathVariable Integer idLote) {
		loteServicios.seguimiento(idLote, authenticatedUserService.getCurrentUser());
		return loteServicios.detalleConCargas(idLote);
	}

	@GetMapping("/{idLote}/financiero")
	public java.util.Map<String,Object> desgloseFinanciero(@PathVariable Integer idLote) {
		return loteServicios.financiero(idLote, authenticatedUserService.getCurrentUser());
	}

	@GetMapping("/{idLote}/seguimiento")
	public Lote seguimiento(@PathVariable Integer idLote) {
		return loteServicios.seguimiento(idLote, authenticatedUserService.getCurrentUser());
	}

    @PatchMapping("/{idLote}/puntos-ruta")
    public Lote guardarPuntosRuta(@PathVariable Integer idLote, @Valid @RequestBody PuntosRutaRequest req) {
        return loteServicios.guardarPuntosRuta(idLote, req, authenticatedUserService.getCurrentUser());
    }

    @PatchMapping("/{idLote}/ubicacion")
    public Lote actualizarUbicacion(@PathVariable Integer idLote, @Valid @RequestBody UbicacionLoteRequest req) {
        return loteServicios.actualizarUbicacion(idLote, req, authenticatedUserService.getCurrentUser());
    }

	@PatchMapping("/{idLote}/confirmar-entrega")
	public Lote confirmarEntrega(@PathVariable Integer idLote, @Valid @RequestBody ConfirmarEntregaRequest req) {
		return loteServicios.confirmarEntrega(idLote, req, authenticatedUserService.getCurrentUser().getIdUsuario());
	}

	@PatchMapping("/{idLote}/reportar-retraso")
	public Lote reportarRetraso(@PathVariable Integer idLote, @Valid @RequestBody ReportarRetrasoRequest req) {
		return loteServicios.reportarRetraso(idLote, req, authenticatedUserService.getCurrentUser().getIdUsuario());
	}
}
