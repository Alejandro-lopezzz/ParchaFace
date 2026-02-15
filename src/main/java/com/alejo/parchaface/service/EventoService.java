package com.alejo.parchaface.service;

import com.alejo.parchaface.dto.CrearEventoDTO;
import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.EstadoEvento;

import java.util.List;

public interface EventoService {

    List<Evento> getAllEventos();

    Evento getEventoById(Integer id);

    Evento saveEvento(Evento evento);

    void deleteEvento(Integer id);

    List<Evento> getEventosPorEstado(EstadoEvento estadoEvento);

    Evento crearEvento(CrearEventoDTO dto, Usuario organizador);
}
