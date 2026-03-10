package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Integer> {

  List<Inscripcion> findByEvento_IdEvento(Integer idEvento);

  boolean existsByEvento_IdEventoAndUsuario_IdUsuario(Integer idEvento, Integer idUsuario);

  long countByEvento_IdEvento(Integer idEvento);

  List<Inscripcion> findByUsuario_IdUsuarioOrderByFechaInscripcionDesc(Integer idUsuario);
}
