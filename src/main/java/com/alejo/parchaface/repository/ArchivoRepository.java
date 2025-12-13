package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.Archivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArchivoRepository extends JpaRepository<Archivo, Integer> {

    @Query("SELECT a FROM Archivo a WHERE a.id_evento.id_evento = :idEvento")
    List<Archivo> findByIdEvento(@Param("idEvento") Integer idEvento);
}
