package com.upc.agrodirecto.repository;

import com.upc.agrodirecto.entidades.SolicitudChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SolicitudChatRepositorio extends JpaRepository<SolicitudChat, Integer> {
    boolean existsByProductor_IdUsuarioAndTransportista_IdUsuarioAndLote_IdLote(Integer p, Integer t, Integer l);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SolicitudChat s WHERE s.idSolicitud = :id")
    java.util.Optional<SolicitudChat> bloquear(@Param("id") Integer id);


	@Query("SELECT sc FROM SolicitudChat sc JOIN FETCH sc.productor p JOIN FETCH sc.transportista t JOIN FETCH sc.lote l " +
			"WHERE (:estado IS NULL OR sc.estadoSolicitud = :estado)")
	List<SolicitudChat> listarPorEstado(@Param("estado") String estado);
}
