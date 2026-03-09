package com.alejo.parchaface.service;

import com.alejo.parchaface.model.Notificacion;
import com.alejo.parchaface.model.Usuario;

import java.util.List;

public interface NotificacionService {

    // ====== lo que ya tenías ======
    Notificacion guardarNotificacion(Notificacion notificacion);
    Notificacion obtenerNotificacionPorId(int id);
    List<Notificacion> obtenerTodas();
    List<Notificacion> obtenerPorUsuario(Usuario usuario);
    Notificacion actualizarNotificacion(Notificacion notificacion);
    void eliminarNotificacion(int id);

    // ====== NUEVO (para JWT / automático) ======
    Notificacion crearNotificacion(Usuario usuario, String mensaje);

    List<Notificacion> obtenerMisNotificaciones(String correo);

    List<Notificacion> obtenerNoLeidas(String correo);

    long contadorNoLeidas(String correo);

    Notificacion marcarLeida(Integer idNotificacion, String correo);

    void marcarTodasLeidas(String correo);
}