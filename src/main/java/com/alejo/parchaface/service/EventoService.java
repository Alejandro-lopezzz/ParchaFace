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

  Evento crearEvento(CrearEventoDTO dto, Usuario organizador);

  Evento crearEvento(CrearEventoForm form, Usuario organizador);

  Evento crearEvento(CrearEventoForm form, Usuario organizador, EstadoEvento estado);

  Evento solicitarCreacionEvento(CrearEventoForm form, Usuario organizador);

  List<Evento> listarPendientesAprobacion();

  Evento aprobarEvento(Integer idEvento);

  Evento rechazarEvento(Integer idEvento, String motivo);

  Evento actualizarEventoYNotificar(Integer idEvento, Evento cambios);

  void eliminarEventoYNotificar(Integer idEvento);
}
