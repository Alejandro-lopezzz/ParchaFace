package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.Estadistica;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadisticaRepository extends JpaRepository<Estadistica, Integer> {

    @Query("SELECT e FROM Estadistica e WHERE e.id_evento.id_evento = :idEvento")
    Optional<Estadistica> findByIdEvento(@Param("idEvento") int idEvento);
}
