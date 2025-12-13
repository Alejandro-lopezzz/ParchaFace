package com.alejo.parchaface.service;

import com.alejo.parchaface.model.Inscripcion;
import java.util.List;

public interface InscripcionService {

    List<Inscripcion> getAllInscripciones();

    Inscripcion getInscripcionById(Integer id);

    Inscripcion createInscripcion(Inscripcion inscripcion);

    Inscripcion updateInscripcion(Integer id, Inscripcion inscripcion);

    void deleteInscripcion(Integer id);
}
