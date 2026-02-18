package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.Inscripcion;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.EstadoInscripcion;
import com.alejo.parchaface.repository.EventoRepository;
import com.alejo.parchaface.repository.InscripcionRepository;
import com.alejo.parchaface.repository.UsuarioRepository;
import com.alejo.parchaface.service.InscripcionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class InscripcionServiceImpl implements InscripcionService {

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Override
    public List<Inscripcion> getAllInscripciones() {
        return inscripcionRepository.findAll();
    }

    @Override
    public Inscripcion getInscripcionById(Integer id) {
        Optional<Inscripcion> optional = inscripcionRepository.findById(id);
        return optional.orElse(null);
    }

    @Override
    public Inscripcion createInscripcion(Inscripcion inscripcion) {
        return inscripcionRepository.save(inscripcion);
    }

    @Override
    public Inscripcion updateInscripcion(Inscripcion inscripcion) {
        if (inscripcion.getIdInscripcion() == null) return null;

        Optional<Inscripcion> optional = inscripcionRepository.findById(inscripcion.getIdInscripcion());
        if (optional.isPresent()) {
            Inscripcion existente = optional.get();
            existente.setUsuario(inscripcion.getUsuario());
            existente.setEvento(inscripcion.getEvento());
            existente.setFechaInscripcion(inscripcion.getFechaInscripcion());
            existente.setEstadoInscripcion(inscripcion.getEstadoInscripcion());
            return inscripcionRepository.save(existente);
        }
        return null;
    }

    @Override
    public void deleteInscripcion(Integer id) {
        inscripcionRepository.deleteById(id);
    }

    // ✅ NUEVO: Inscripción con JWT
    @Override
    @Transactional
    public Inscripcion inscribirseAEvento(Integer idEvento, String correo) {

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));

        // ✅ 0) El organizador NO se puede inscribir
        if (evento.getOrganizador() != null
                && evento.getOrganizador().getIdUsuario() != null
                && evento.getOrganizador().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El organizador no puede inscribirse a su propio evento");
        }


        // 1) evitar duplicado
        boolean yaInscrito = inscripcionRepository.existsByEvento_IdEventoAndUsuario_IdUsuario(idEvento, usuario.getIdUsuario());
        if (yaInscrito) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya estás inscrito en este evento");
        }

        // 2) validar cupo (si cupo=0 lo tratamos como sin cupo / lleno)
        Integer cupo = evento.getCupo();
        long inscritos = inscripcionRepository.countByEvento_IdEvento(idEvento);

        if (cupo != null && cupo > 0 && inscritos >= cupo) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El evento ya no tiene cupo");
        }

        // 3) crear inscripción
        Inscripcion ins = new Inscripcion();
        ins.setUsuario(usuario);
        ins.setEvento(evento);
        ins.setFechaInscripcion(LocalDate.now());
        ins.setEstadoInscripcion(EstadoInscripcion.vigente);

        return inscripcionRepository.save(ins);
    }
}
