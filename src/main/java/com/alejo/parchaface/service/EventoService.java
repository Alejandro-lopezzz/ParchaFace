package com.alejo.parchaface.service;

import com.alejo.parchaface.dto.CrearEventoDTO;
import com.alejo.parchaface.dto.CrearEventoForm;
import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.EstadoEvento;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EventoService {

    List<Evento> getAllEventos();

    Evento getEventoById(Integer id);

    Evento saveEvento(Evento evento);

    void deleteEvento(Integer id);

    List<Evento> getEventosPorEstado(EstadoEvento estadoEvento);

    // JSON (viejo)
    Evento crearEvento(CrearEventoDTO dto, Usuario organizador);

    // multipart/form-data (nuevo)
    Evento crearEvento(CrearEventoForm form, MultipartFile imagenPortada, Usuario organizador);

    // multipart/form-data con estado específico (para borradores)
    Evento crearEvento(CrearEventoForm form, MultipartFile imagenPortada, Usuario organizador, EstadoEvento estado);
}
