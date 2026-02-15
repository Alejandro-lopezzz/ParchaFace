package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.CrearEventoDTO;
import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.EstadoEvento;
import com.alejo.parchaface.service.EventoService;
import com.alejo.parchaface.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/eventos")
public class EventoController {

    private final EventoService eventoService;
    private final UsuarioService usuarioService;

    public EventoController(EventoService eventoService, UsuarioService usuarioService) {
        this.eventoService = eventoService;
        this.usuarioService = usuarioService;
    }

    // =========================
    // GET TODOS (protegido por config global: anyRequest().authenticated())
    // =========================
    @GetMapping
    public List<Evento> obtenerTodos() {
        return eventoService.getAllEventos();
    }

    // =========================
    // GET POR ID
    // =========================
    @GetMapping("/{id}")
    public Evento obtenerPorId(@PathVariable Integer id) {
        return eventoService.getEventoById(id);
    }

    // =========================
    // POST CREAR — JWT (USUARIO o ADMINISTRADOR)
    // =========================
    @PostMapping
    @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<Evento> crearEvento(
            @Valid @RequestBody CrearEventoDTO dto,
            Authentication authentication) {

        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        String correo = authentication.getName();
        Usuario organizador = usuarioService.getUsuarioPorCorreo(correo);

        Evento evento = eventoService.crearEvento(dto, organizador);

        return ResponseEntity.status(HttpStatus.CREATED).body(evento);
    }

    // =========================
    // GET POR ESTADO
    // =========================
    @GetMapping("/estado/{estado}")
    public List<Evento> obtenerPorEstado(@PathVariable EstadoEvento estado) {
        return eventoService.getEventosPorEstado(estado);
    }

    // =========================
    // PUT ACTUALIZAR (ejemplo: solo ADMIN)
    // =========================
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public Evento actualizar(@PathVariable Integer id,
                             @RequestBody Evento evento) {

        evento.setIdEvento(id);
        return eventoService.saveEvento(evento);
    }

    // =========================
    // DELETE — ADMIN JWT
    // =========================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        eventoService.deleteEvento(id);
        return ResponseEntity.noContent().build();
    }
}
