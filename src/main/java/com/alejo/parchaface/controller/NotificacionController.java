package com.alejo.parchaface.controller;

import com.alejo.parchaface.model.Notificacion;
import com.alejo.parchaface.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    // ✅ Mis notificaciones (todas)
    @GetMapping
    @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
    public List<Notificacion> misNotificaciones(Authentication auth) {
        return notificacionService.obtenerMisNotificaciones(auth.getName());
    }

    // ✅ Mis notificaciones no leídas
    @GetMapping("/no-leidas")
    @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
    public List<Notificacion> noLeidas(Authentication auth) {
        return notificacionService.obtenerNoLeidas(auth.getName());
    }

    // ✅ Contador de no leídas
    @GetMapping("/contador-no-leidas")
    @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
    public long contadorNoLeidas(Authentication auth) {
        return notificacionService.contadorNoLeidas(auth.getName());
    }

    // ✅ Marcar UNA como leída
    @PutMapping("/{id}/leer")
    @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
    public Notificacion marcarLeida(@PathVariable Integer id, Authentication auth) {
        return notificacionService.marcarLeida(id, auth.getName());
    }

    // ✅ Marcar TODAS como leídas
    @PutMapping("/leer-todas")
    @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
    public void leerTodas(Authentication auth) {
        notificacionService.marcarTodasLeidas(auth.getName());
    }
}