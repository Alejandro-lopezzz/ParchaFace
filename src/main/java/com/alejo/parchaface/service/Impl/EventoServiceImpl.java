package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.repository.EventoRepository;
import com.alejo.parchaface.service.EventoService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class EventoServiceImpl implements EventoService {

    private final EventoRepository eventoRepository;

    public EventoServiceImpl(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    @Override
    public List<Evento> getAllEventos() {
        return eventoRepository.findAll();
    }

    @Override
    public Evento getEventoById(Integer id) {
        Optional<Evento> evento = eventoRepository.findById(id);
        return evento.orElse(null);
    }

    @Override
    public Evento saveEvento(Evento evento) {
        return eventoRepository.save(evento);
    }

    @Override
    public void deleteEvento(Integer id) {
        eventoRepository.deleteById(id);
    }
}
