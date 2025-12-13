package com.alejo.parchaface.controller;

import com.alejo.parchaface.model.Notificacion;
import com.alejo.parchaface.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    @GetMapping
    public List<Notificacion> obtenerTodas() {
        return notificacionService.obtenerTodas();
    }

    @GetMapping("/{id}")
    public Notificacion obtenerPorId(@PathVariable int id) {
        return notificacionService.obtenerNotificacionPorId(id);
    }

    @PostMapping
    public Notificacion guardar(@RequestBody Notificacion notificacion) {
        return notificacionService.guardarNotificacion(notificacion);
    }

    @PutMapping("/{id}")
    public Notificacion actualizar(@PathVariable int id, @RequestBody Notificacion notificacion) {
        // Asegurarse de que el id se mantenga en el objeto antes de guardar
        notificacion.setId_notificacion(id);
        return notificacionService.actualizarNotificacion(notificacion);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable int id) {
        notificacionService.eliminarNotificacion(id);
    }
}
