package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.CrearEventoDTO;
import com.alejo.parchaface.dto.CrearEventoForm;
import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.EstadoEvento;
import com.alejo.parchaface.service.EventoService;
import com.alejo.parchaface.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    // GET TODOS
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
    // POST CREAR — JSON (legacy / DTO viejo)
    // =========================
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<Evento> crearEventoJson(
            @Valid @RequestBody CrearEventoDTO dto,
            Authentication authentication
    ) {
        Usuario organizador = getOrganizador(authentication);
        Evento evento = eventoService.crearEvento(dto, organizador);
        return ResponseEntity.status(HttpStatus.CREATED).body(evento);
    }

    // =========================
    // POST CREAR — MULTIPART/FORM-DATA (nuevo, con archivo real)
    // =========================
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<Evento> crearEventoForm(
            @Valid @ModelAttribute CrearEventoForm form,
            Authentication authentication
    ) {
        Usuario organizador = getOrganizador(authentication);

        // ✅ Si tu form incluye "MultipartFile imagenPortada", se obtiene así:
        var imagen = form.getImagenPortada();

        Evento evento = eventoService.crearEvento(form, imagen, organizador);
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
    // PUT (ADMIN)
    // =========================
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public Evento actualizar(@PathVariable Integer id, @RequestBody Evento evento) {
        evento.setIdEvento(id);
        return eventoService.saveEvento(evento);
    }

    // =========================
    // DELETE (ADMIN)
    // =========================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        eventoService.deleteEvento(id);
        return ResponseEntity.noContent().build();
    }

    // =========================
    // Helper auth
    // =========================
    private Usuario getOrganizador(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        String correo = authentication.getName();
        return usuarioService.getUsuarioPorCorreo(correo);
    }
}
