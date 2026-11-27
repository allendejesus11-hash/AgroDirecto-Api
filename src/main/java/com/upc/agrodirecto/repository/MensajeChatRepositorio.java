package com.upc.agrodirecto.repository;

import com.upc.agrodirecto.entidades.MensajeChat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensajeChatRepositorio extends JpaRepository<MensajeChat, Integer> {
    java.util.List<MensajeChat> findBySolicitud_IdSolicitudOrderByFechaEnvioAscIdMensajeAsc(Integer idSolicitud);
}
