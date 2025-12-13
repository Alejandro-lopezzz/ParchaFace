package com.alejo.parchaface.service;

import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.Inscripcion;

import java.util.List;

public interface EventoService {
    List<Evento> getAllEventos();
    Evento getEventoById(Integer id);
    Evento saveEvento(Evento evento);
    void deleteEvento(Integer id);
}
