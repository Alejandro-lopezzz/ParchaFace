package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.CreateEventoCommentRequest;
import com.alejo.parchaface.dto.EventoCommentResponse;
import com.alejo.parchaface.dto.UpdateEventoCommentRequest;
import com.alejo.parchaface.service.EventoCommentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EventoCommentController {

    private final EventoCommentService service;

    public EventoCommentController(EventoCommentService service) {
        this.service = service;
    }

    // ✅ Listar comentarios de un evento (puede ser público si tu SecurityConfig lo permite)
    @GetMapping("/eventos/{eventoId}/comentarios")
    public Page<EventoCommentResponse> listar(
            @PathVariable Integer eventoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.listar(eventoId, page, size);
    }

    // ✅ Crear comentario (requiere login)
    @PostMapping("/eventos/{eventoId}/comentarios")
    public ResponseEntity<EventoCommentResponse> crear(
            @PathVariable Integer eventoId,
            @Valid @RequestBody CreateEventoCommentRequest request,
            Authentication auth
    ) {
        String correo = auth.getName(); // normalmente tu JWT setea el correo como principal
        return ResponseEntity.ok(service.crear(eventoId, request, correo));
    }

    // ✅ Editar comentario (solo el dueño)
    @PutMapping("/comentarios/{commentId}")
    public ResponseEntity<EventoCommentResponse> actualizar(
            @PathVariable Integer commentId,
            @Valid @RequestBody UpdateEventoCommentRequest request,
            Authentication auth
    ) {
        String correo = auth.getName();
        return ResponseEntity.ok(service.actualizar(commentId, request, correo));
    }

    // ✅ Eliminar comentario (solo el dueño)
    @DeleteMapping("/comentarios/{commentId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Integer commentId,
            Authentication auth
    ) {
        String correo = auth.getName();
        service.eliminar(commentId, correo);
        return ResponseEntity.noContent().build();
    }
}