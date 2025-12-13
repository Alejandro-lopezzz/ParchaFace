package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface InscripcionRepository extends JpaRepository<Inscripcion, Integer> {

    @Query("SELECT i FROM Inscripcion i WHERE i.evento.id_evento = :idEvento")
    List<Inscripcion> findByEventoId(@Param("idEvento") Integer idEvento);

}


