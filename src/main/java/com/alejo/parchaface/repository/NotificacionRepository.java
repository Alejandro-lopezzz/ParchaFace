package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.Notificacion;
import com.alejo.parchaface.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
    List<Notificacion> findByUsuario(Usuario usuario); // ✔ correcto
}
