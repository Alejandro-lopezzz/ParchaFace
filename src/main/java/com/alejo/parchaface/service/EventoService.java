package com.alejo.parchaface.service;

import com.alejo.parchaface.dto.CrearEventoDTO;
import com.alejo.parchaface.dto.CrearEventoForm;
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

    List<Evento> getEventosPublicos();

    // Legacy JSON
    Evento crearEvento(CrearEventoDTO dto, Usuario organizador);

    // Nuevo flujo multipart/form-data
    Evento crearEvento(CrearEventoForm form, Usuario organizador);

    Evento crearEvento(CrearEventoForm form, Usuario organizador, EstadoEvento estado);

    Evento actualizarEventoYNotificar(Integer idEvento, Evento cambios);

    void eliminarEventoYNotificar(Integer idEvento);
}