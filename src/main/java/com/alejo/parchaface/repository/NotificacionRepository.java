package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.Notificacion;
import com.alejo.parchaface.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    List<Notificacion> findByUsuario(Usuario usuario);

    // ✅ OJO: fechaEnvio (camelCase)
    List<Notificacion> findByUsuario_IdUsuarioOrderByFechaEnvioDesc(Integer idUsuario);

    List<Notificacion> findByUsuario_IdUsuarioAndLeidoFalseOrderByFechaEnvioDesc(Integer idUsuario);

    long countByUsuario_IdUsuarioAndLeidoFalse(Integer idUsuario);

    void deleteByUsuario_IdUsuario(Integer usuarioId);
}
