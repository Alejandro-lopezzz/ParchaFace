package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReporteRepository extends JpaRepository<Reporte, Integer> {

    List<Reporte> findByAdmin_IdUsuario(Integer idUsuario);

}
