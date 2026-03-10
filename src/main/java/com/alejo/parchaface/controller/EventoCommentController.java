package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.CreateEventoCommentRequest;
import com.alejo.parchaface.dto.EventoCommentResponse;
import com.alejo.parchaface.dto.UpdateEventoCommentRequest;
import com.alejo.parchaface.service.EventoCommentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @PostMapping(value = "/eventos/{eventoId}/comentarios", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventoCommentResponse> crear(
            @PathVariable Integer eventoId,
            @RequestPart(value = "contenido", required = false) String contenido,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen,
            Authentication auth
    ) {
        String correo = auth.getName();
        CreateEventoCommentRequest request = new CreateEventoCommentRequest(contenido);
        return ResponseEntity.ok(service.crear(eventoId, request, imagen, correo));
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