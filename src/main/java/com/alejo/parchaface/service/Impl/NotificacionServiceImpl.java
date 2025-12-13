package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.model.Notificacion;
import com.alejo.parchaface.repository.NotificacionRepository;
import com.alejo.parchaface.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NotificacionServiceImpl implements NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

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
    public Notificacion actualizarNotificacion(Notificacion notificacion) {
        return notificacionRepository.save(notificacion);
    }

    @Override
    public void eliminarNotificacion(int id) {
        notificacionRepository.deleteById(id);
    }
}
