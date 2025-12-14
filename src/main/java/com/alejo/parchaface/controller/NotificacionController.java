package com.alejo.parchaface.controller;

import com.alejo.parchaface.model.Notificacion;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.service.NotificacionService;
import com.alejo.parchaface.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private UsuarioService usuarioService; // Para obtener usuario por ID

    // Obtener todas las notificaciones
    @GetMapping
    public List<Notificacion> obtenerTodas() {
        return notificacionService.obtenerTodas();
    }

    // Obtener notificación por ID
    @GetMapping("/{id}")
    public Notificacion obtenerPorId(@PathVariable int id) {
        return notificacionService.obtenerNotificacionPorId(id);
    }

    // Obtener notificaciones de un usuario específico
    @GetMapping("/usuario/{idUsuario}")
    public List<Notificacion> obtenerPorUsuario(@PathVariable int idUsuario) {
        Usuario usuario = usuarioService.getUsuarioById(idUsuario);
        if (usuario == null) {
            return List.of(); // Retorna lista vacía si no existe el usuario
        }
        return notificacionService.obtenerPorUsuario(usuario);
    }

    // Crear nueva notificación
    @PostMapping
    public Notificacion guardar(@RequestBody Notificacion notificacion) {
        return notificacionService.guardarNotificacion(notificacion);
    }

    // Actualizar notificación existente
    @PutMapping("/{id}")
    public Notificacion actualizar(@PathVariable int id, @RequestBody Notificacion notificacion) {
        notificacion.setId_notificacion(id);
        return notificacionService.actualizarNotificacion(notificacion);
    }

    // Eliminar notificación por ID
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable int id) {
        notificacionService.eliminarNotificacion(id);
    }
}
