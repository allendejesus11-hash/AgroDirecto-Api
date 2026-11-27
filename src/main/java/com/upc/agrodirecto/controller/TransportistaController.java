package com.upc.agrodirecto.controller;

import com.upc.agrodirecto.entidades.Lote;
import com.upc.agrodirecto.servicios.LoteServicios;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transportistas")
public class TransportistaController {
	@Autowired
	private LoteServicios loteServicios;
	@Autowired private com.upc.agrodirecto.security.service.AuthenticatedUserService authenticatedUserService;

	@GetMapping("/{idTransportista}/lotes")
	public List<Lote> historial(@PathVariable Integer idTransportista, @RequestParam(required = false) String estado) {
        if (!authenticatedUserService.getCurrentUser().getIdUsuario().equals(idTransportista))
            throw new org.springframework.security.access.AccessDeniedException("Solo puedes consultar tus propios lotes");
        return loteServicios.historialTransportista(idTransportista, estado);
	}
}
