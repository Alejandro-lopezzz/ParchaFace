package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.PagoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoEventoRepository extends JpaRepository<PagoEvento, Integer> {

    Optional<PagoEvento> findByReferencia(String referencia);

    List<PagoEvento> findByUsuario_IdUsuarioOrderByFechaCreacionDesc(Integer idUsuario);
}