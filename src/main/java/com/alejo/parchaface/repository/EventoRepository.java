package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.enums.EstadoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Integer> {
    List<Evento> findByEstadoEvento(EstadoEvento estadoEvento);
}
