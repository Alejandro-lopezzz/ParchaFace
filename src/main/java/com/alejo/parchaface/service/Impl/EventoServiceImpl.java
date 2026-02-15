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

        // ======================
        // Básicos
        // ======================
        evento.setTitulo(dto.getTitulo());
        evento.setDescripcion(dto.getDescripcion());
        evento.setCategoria(dto.getCategoria());

        // ======================
        // Imagen portada
        // ======================
        evento.setImagenPortadaUrl(dto.getImagenPortadaUrl());
        evento.setImagenPortadaContentType(dto.getImagenPortadaContentType());

        // ======================
        // Fecha / horas (BD: fecha DATETIME, hora TIME, hora_fin TIME)
        // ======================
        // Guardamos el inicio como datetime(6) usando fecha + horaInicio
        evento.setFecha(LocalDateTime.of(dto.getFecha(), dto.getHoraInicio()));
        evento.setHoraInicio(dto.getHoraInicio());
        evento.setHoraFin(dto.getHoraFin()); // puede ser null (BD lo permite)

        // ======================
        // Modalidad online
        // ======================
        boolean enLinea = Boolean.TRUE.equals(dto.getEventoEnLinea());
        evento.setEventoEnLinea(enLinea);

        if (enLinea) {
            evento.setUrlVirtual(dto.getUrlVirtual());

            // presencial -> null
            evento.setUbicacion(null);
            evento.setNombreLugar(null);
            evento.setDireccionCompleta(null);
            evento.setCiudad(null);
        } else {
            evento.setUrlVirtual(null);

            evento.setUbicacion(dto.getUbicacion()); // columna lugar
            evento.setNombreLugar(dto.getNombreLugar());
            evento.setDireccionCompleta(dto.getDireccionCompleta());
            evento.setCiudad(dto.getCiudad());
        }

        // ======================
        // Cupo
        // ======================
        evento.setCupo(dto.getCupo());

        // ======================
        // Gratis / precio
        // ======================
        boolean gratuito = Boolean.TRUE.equals(dto.getEventoGratuito());
        evento.setEventoGratuito(gratuito);

        if (gratuito) {
            evento.setPrecio(null);
        } else {
            evento.setPrecio(dto.getPrecio());
        }

        // ======================
        // Contacto
        // ======================
        evento.setEmailContacto(dto.getEmailContacto());
        evento.setTelefonoContacto(dto.getTelefonoContacto());
        evento.setSitioWeb(dto.getSitioWeb());

        // ======================
        // Privacidad
        // ======================
        boolean publico = Boolean.TRUE.equals(dto.getEventoPublico());
        evento.setEventoPublico(publico);

        if (publico) {
            evento.setDetallePrivado(null);
        } else {
            evento.setDetallePrivado(dto.getDetallePrivado());
        }

        // ======================
        // Config del evento
        // ======================
        evento.setPermitirComentarios(Boolean.TRUE.equals(dto.getPermitirComentarios()));
        evento.setRecordatoriosAutomaticos(Boolean.TRUE.equals(dto.getRecordatoriosAutomaticos()));

        // ======================
        // Defaults / relaciones
        // ======================
        evento.setEstadoEvento(EstadoEvento.activo);
        evento.setOrganizador(organizador);

        return eventoRepository.save(evento);
    }
}
