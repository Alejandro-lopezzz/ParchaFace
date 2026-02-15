package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.model.Inscripcion;
import com.alejo.parchaface.repository.InscripcionRepository;
import com.alejo.parchaface.service.InscripcionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InscripcionServiceImpl implements InscripcionService {

    @Autowired
    private InscripcionRepository inscripcionRepository;

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
        // Solo guarda la inscripción, asegurando que usuario y evento existan
        return inscripcionRepository.save(inscripcion);
    }

    @Override
    public Inscripcion updateInscripcion(Inscripcion inscripcion) {
        if (inscripcion.getIdInscripcion() == null) {
            return null; // o lanzar excepción, porque no hay id para actualizar
        }

        Optional<Inscripcion> optional = inscripcionRepository.findById(inscripcion.getIdInscripcion());
        if (optional.isPresent()) {
            Inscripcion existente = optional.get();
            // Actualizamos los campos
            existente.setUsuario(inscripcion.getUsuario());
            existente.setEvento(inscripcion.getEvento());
            existente.setFechaInscripcion(inscripcion.getFechaInscripcion());
            existente.setEstadoInscripcion(inscripcion.getEstadoInscripcion());
            return inscripcionRepository.save(existente);
        }
        return null; // o lanzar excepción si no existe
    }

    @Override
    public void deleteInscripcion(Integer id) {
        inscripcionRepository.deleteById(id);
    }
}
