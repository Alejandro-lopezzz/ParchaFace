package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.Inscripcion;
import com.alejo.parchaface.model.enums.EstadoInscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Integer> {

  List<Inscripcion> findByEvento_IdEvento(Integer idEvento);

  boolean existsByEvento_IdEventoAndUsuario_IdUsuario(Integer idEvento, Integer idUsuario);

  long countByEvento_IdEvento(Integer idEvento);

  List<Inscripcion> findByUsuario_IdUsuarioOrderByFechaInscripcionDesc(Integer idUsuario);

  Optional<Inscripcion> findByEvento_IdEventoAndUsuario_IdUsuario(Integer idEvento, Integer idUsuario);

  long countByEvento_IdEventoAndEstadoInscripcion(Integer idEvento, EstadoInscripcion estadoInscripcion);

  List<Inscripcion> findByUsuario_IdUsuarioAndEstadoInscripcionOrderByFechaInscripcionDesc(
          Integer idUsuario,
          EstadoInscripcion estadoInscripcion
  );

  Optional<Inscripcion> findByEvento_IdEventoAndUsuario_IdUsuarioAndEstadoInscripcion(
          Integer idEvento,
          Integer idUsuario,
          EstadoInscripcion estadoInscripcion
  );

  boolean existsByEvento_IdEventoAndUsuario_IdUsuarioAndEstadoInscripcion(
          Integer idEvento,
          Integer idUsuario,
          EstadoInscripcion estadoInscripcion
  );
}
