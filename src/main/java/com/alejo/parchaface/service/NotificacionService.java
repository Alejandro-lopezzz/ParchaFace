package com.alejo.parchaface.service;

import com.alejo.parchaface.model.Notificacion;
import java.util.List;

public interface NotificacionService {
    Notificacion guardarNotificacion(Notificacion notificacion);
    Notificacion obtenerNotificacionPorId(int id);
    List<Notificacion> obtenerTodas();
    Notificacion actualizarNotificacion(Notificacion notificacion);
    void eliminarNotificacion(int id);
}
