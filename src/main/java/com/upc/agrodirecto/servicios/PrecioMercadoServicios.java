package com.upc.agrodirecto.servicios;

import com.upc.agrodirecto.entidades.PrecioMercado;
import com.upc.agrodirecto.repository.PrecioMercadoRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PrecioMercadoServicios {
	@Autowired
	private PrecioMercadoRepositorio precioMercadoRepositorio;

	@Autowired private com.upc.agrodirecto.repository.CultivoRepositorio cultivos;
    public record Fila(Integer idCultivo, Double precioPorKg, LocalDate fecha, String fuente) {}
    public List<PrecioMercado> historial(Integer id,LocalDate desde,LocalDate hasta){
        if(hasta==null)hasta=precioMercadoRepositorio.ultimaFechaConFuente();
        if(hasta==null)return List.of();
        if(desde==null)desde=hasta.minusDays(29);
        if(desde.isAfter(hasta)||desde.plusDays(366).isBefore(hasta))throw new IllegalArgumentException("Rango inválido; máximo 366 días");
        return precioMercadoRepositorio.historial(id,desde,hasta);
    }
    @org.springframework.transaction.annotation.Transactional
    public int importar(List<Fila> filas){
        if(filas==null||filas.isEmpty()||filas.size()>500)throw new IllegalArgumentException("Envía de 1 a 500 filas");
        for(Fila f:filas){
            if(f==null||f.idCultivo()==null||f.fecha()==null||f.fecha().isAfter(LocalDate.now(java.time.ZoneId.of("America/Lima")))||f.precioPorKg()==null||!Double.isFinite(f.precioPorKg())||f.precioPorKg()<=0||f.fuente()==null||f.fuente().isBlank()||f.fuente().length()>500)throw new IllegalArgumentException("Cada fila requiere cultivo, fecha válida, precio positivo y fuente (máx. 500 caracteres)");
            var cultivo=cultivos.findById(f.idCultivo()).orElseThrow();
            PrecioMercado p=precioMercadoRepositorio.findByCultivo_IdCultivoAndFechaPrecio(f.idCultivo(),f.fecha()).orElseGet(PrecioMercado::new);
            p.setCultivo(cultivo);p.setFechaPrecio(f.fecha());p.setPrecioPorKg(f.precioPorKg());p.setFuente(f.fuente().trim());precioMercadoRepositorio.save(p);
        } return filas.size();
    }
    public List<PrecioMercado> listarPorFecha(LocalDate fecha) {
		LocalDate consulta = (fecha != null) ? fecha : LocalDate.now();
		return precioMercadoRepositorio.listarPorFecha(consulta);
	}

	public PrecioMercado ultimoPrecio(Integer idCultivo) {
		List<PrecioMercado> lista = precioMercadoRepositorio.listarPorCultivoOrdenado(idCultivo);
		if (lista.isEmpty()) {
			throw new IllegalStateException("El cultivo no tiene precios registrados");
		}
		return lista.get(0);
	}
}
