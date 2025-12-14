package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.Publicidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PublicidadRepository extends JpaRepository<Publicidad, Integer> {

    List<Publicidad> findByEvento_IdEvento(Integer idEvento);

}




