package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.enums.EstadoEvento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventoRepository extends JpaRepository<Evento, Integer> {

  List<Evento> findByEstadoEvento(EstadoEvento estadoEvento);

  List<Evento> findByOrganizador_IdUsuarioOrderByFechaCreacionDesc(Integer idUsuario);

  @EntityGraph(attributePaths = { "organizador" })
  Optional<Evento> findWithOrganizadorByIdEvento(Integer idEvento);
}
