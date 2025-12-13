package com.alejo.parchaface.service;

import com.alejo.parchaface.model.Notificacion;
import com.alejo.parchaface.model.Usuario;
import java.util.List;

public interface NotificacionService {
    Notificacion guardarNotificacion(Notificacion notificacion);
    Notificacion obtenerNotificacionPorId(int id);
    List<Notificacion> obtenerTodas();
    List<Notificacion> obtenerPorUsuario(Usuario usuario); // nuevo
    Notificacion actualizarNotificacion(Notificacion notificacion);
    void eliminarNotificacion(int id);
}
