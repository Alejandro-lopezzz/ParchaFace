package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.dto.CrearEventoDTO;
import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.EstadoEvento;
import com.alejo.parchaface.repository.EventoRepository;
import com.alejo.parchaface.service.EventoService;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EventoServiceImpl implements EventoService {

    private final EventoRepository eventoRepository;

    public EventoServiceImpl(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    // =========================
    // GET TODOS
    // =========================
    @Override
    public List<Evento> getAllEventos() {
        return eventoRepository.findAll();
    }

    // =========================
    // GET POR ID
    // =========================
    @Override
    public Evento getEventoById(Integer id) {
        Optional<Evento> evento = eventoRepository.findById(id);
        return evento.orElse(null);
    }

    // =========================
    // SAVE
    // =========================
    @Override
    public Evento saveEvento(Evento evento) {
        return eventoRepository.save(evento);
    }

    // =========================
    // DELETE
    // =========================
    @Override
    public void deleteEvento(Integer id) {
        eventoRepository.deleteById(id);
    }

    // =========================
    // GET POR ESTADO
    // =========================
    @Override
    public List<Evento> getEventosPorEstado(EstadoEvento estadoEvento) {
        return eventoRepository.findByEstadoEvento(estadoEvento);
    }

    // =========================
    // CREAR EVENTO — JWT
    // =========================
    @Override
    public Evento crearEvento(CrearEventoDTO dto, Usuario organizador) {

        Evento evento = new Evento();

        evento.setTitulo(dto.getTitulo());
        evento.setDescripcion(dto.getDescripcion());

        evento.setFecha(LocalDateTime.of(dto.getFecha(), dto.getHora()));
        evento.setHora(dto.getHora());

        evento.setUbicacion(dto.getUbicacion());
        evento.setCupo(dto.getCupo());
        evento.setCategoria(dto.getCategoria());

        evento.setEstadoEvento(EstadoEvento.activo);
        evento.setOrganizador(organizador);

        return eventoRepository.save(evento);
    }

}
