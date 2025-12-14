package com.alejo.parchaface.controller;

import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.EstadoEvento;
import com.alejo.parchaface.security.JwtUtil;
import com.alejo.parchaface.service.EventoService;
import com.alejo.parchaface.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/eventos")
public class EventoController {

    @Autowired
    private EventoService eventoService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public List<Evento> obtenerTodos() {
        return eventoService.getAllEventos();
    }

    @GetMapping("/{id}")
    public Evento obtenerPorId(@PathVariable Integer id) {
        return eventoService.getEventoById(id);
    }

    @PostMapping
    public Evento guardar(@RequestBody Evento evento, @RequestHeader("Authorization") String authHeader) {
        // Extraer token
        String token = authHeader.replace("Bearer ", "");
        String correo = JwtUtil.getCorreoFromToken(token);

        // Buscar usuario por correo
        Usuario organizador = usuarioService.getUsuarioPorCorreo(correo);
        evento.setOrganizador(organizador);

        return eventoService.saveEvento(evento);
    }

    @GetMapping("/estado/{estado}")
    public List<Evento> obtenerPorEstado(@PathVariable EstadoEvento estado) {
        return eventoService.getEventosPorEstado(estado);
    }

    @PutMapping("/{id}")
    public Evento actualizar(@PathVariable Integer id, @RequestBody Evento evento) {
        evento.setIdEvento(id);
        return eventoService.saveEvento(evento);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String correo = JwtUtil.getCorreoFromToken(token);

        Usuario usuario = usuarioService.getUsuarioPorCorreo(correo);
        // Solo administradores pueden eliminar
        if (usuario.getRol() != com.alejo.parchaface.model.enums.Rol.ADMINISTRADOR) {
            throw new RuntimeException("No tienes permiso para eliminar eventos");
        }

        eventoService.deleteEvento(id);
    }
}
