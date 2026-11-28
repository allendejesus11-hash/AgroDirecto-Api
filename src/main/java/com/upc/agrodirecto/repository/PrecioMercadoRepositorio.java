package com.upc.agrodirecto.repository;

import com.upc.agrodirecto.entidades.PrecioMercado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PrecioMercadoRepositorio extends JpaRepository<PrecioMercado, Integer> {
    @Query("SELECT pm FROM PrecioMercado pm JOIN FETCH pm.cultivo WHERE pm.fuente IS NOT NULL AND (:id IS NULL OR pm.cultivo.idCultivo=:id) AND pm.fechaPrecio >= :desde AND pm.fechaPrecio <= :hasta ORDER BY pm.fechaPrecio ASC")
    List<PrecioMercado> historial(@Param("id") Integer id,@Param("desde") LocalDate desde,@Param("hasta") LocalDate hasta);
    @Query("SELECT MAX(pm.fechaPrecio) FROM PrecioMercado pm WHERE pm.fuente IS NOT NULL")
    LocalDate ultimaFechaConFuente();
    Optional<PrecioMercado> findByCultivo_IdCultivoAndFechaPrecio(Integer id,LocalDate fecha);


	@Query("SELECT pm FROM PrecioMercado pm JOIN FETCH pm.cultivo cu WHERE pm.fechaPrecio = :fecha")
	List<PrecioMercado> listarPorFecha(@Param("fecha") LocalDate fecha);

	@Query("SELECT pm FROM PrecioMercado pm JOIN FETCH pm.cultivo cu WHERE pm.cultivo.idCultivo = :idCultivo " +
			"ORDER BY pm.fechaPrecio DESC")
	List<PrecioMercado> listarPorCultivoOrdenado(@Param("idCultivo") Integer idCultivo);
}
