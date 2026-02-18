package com.alejo.parchaface.service;

import com.alejo.parchaface.model.Inscripcion;
import java.util.List;

public interface InscripcionService {

    List<Inscripcion> getAllInscripciones();

    Inscripcion getInscripcionById(Integer id);

    Inscripcion createInscripcion(Inscripcion inscripcion);

    Inscripcion updateInscripcion(Inscripcion inscripcion);

    Inscripcion inscribirseAEvento(Integer idEvento, String correo);

    void deleteInscripcion(Integer id);
}
