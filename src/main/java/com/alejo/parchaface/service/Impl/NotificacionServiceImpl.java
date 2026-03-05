package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.model.Notificacion;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.repository.NotificacionRepository;
import com.alejo.parchaface.repository.UsuarioRepository;
import com.alejo.parchaface.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class NotificacionServiceImpl implements NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public Notificacion guardarNotificacion(Notificacion notificacion) {
        return notificacionRepository.save(notificacion);
    }

    @Override
    public Notificacion obtenerNotificacionPorId(int id) {
        Optional<Notificacion> optional = notificacionRepository.findById(id);
        return optional.orElse(null);
    }

    @Override
    public List<Notificacion> obtenerTodas() {
        return notificacionRepository.findAll();
    }

    @Override
    public List<Notificacion> obtenerPorUsuario(Usuario usuario) {
        return notificacionRepository.findByUsuario(usuario);
    }

    @Override
    public Notificacion actualizarNotificacion(Notificacion notificacion) {
        return notificacionRepository.save(notificacion);
    }

    @Override
    public void eliminarNotificacion(int id) {
        notificacionRepository.deleteById(id);
    }

    // ====== NUEVO ======
    @Override
    public Notificacion crearNotificacion(Usuario usuario, String mensaje) {
        Notificacion n = new Notificacion();
        n.setUsuario(usuario);
        n.setMensaje(mensaje);
        n.setLeido(false);
        return notificacionRepository.save(n);
    }

    @Override
    public List<Notificacion> obtenerMisNotificaciones(String correo) {
        Usuario u = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        return notificacionRepository.findByUsuario_IdUsuarioOrderByFechaEnvioDesc(u.getIdUsuario());
    }

    @Override
    public List<Notificacion> obtenerNoLeidas(String correo) {
        Usuario u = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        return notificacionRepository.findByUsuario_IdUsuarioAndLeidoFalseOrderByFechaEnvioDesc(u.getIdUsuario());
    }

    @Override
    public long contadorNoLeidas(String correo) {
        Usuario u = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        return notificacionRepository.countByUsuario_IdUsuarioAndLeidoFalse(u.getIdUsuario());
    }

    @Override
    public Notificacion marcarLeida(Integer idNotificacion, String correo) {
        Usuario u = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        Notificacion n = notificacionRepository.findById(idNotificacion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no encontrada"));

        if (!n.getUsuario().getIdUsuario().equals(u.getIdUsuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        n.setLeido(true);
        return notificacionRepository.save(n);
    }

    @Override
    public void marcarTodasLeidas(String correo) {
        Usuario u = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        List<Notificacion> lista = notificacionRepository
                .findByUsuario_IdUsuarioAndLeidoFalseOrderByFechaEnvioDesc(u.getIdUsuario());

        for (Notificacion n : lista) {
            n.setLeido(true);
        }

        notificacionRepository.saveAll(lista);
    }
}