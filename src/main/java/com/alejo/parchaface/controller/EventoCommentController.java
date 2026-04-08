package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.CreateEventoCommentRequest;
import com.alejo.parchaface.dto.EventoCommentResponse;
import com.alejo.parchaface.dto.UpdateEventoCommentRequest;
import com.alejo.parchaface.service.EventoCommentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class EventoCommentController {

  private final EventoCommentService eventoCommentService;

  public EventoCommentController(
    EventoCommentService eventoCommentService
  ) {
    this.eventoCommentService = eventoCommentService;
  }

  @GetMapping("/eventos/{eventoId}/comentarios")
  public Page<EventoCommentResponse> listar(
    @PathVariable Integer eventoId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    return eventoCommentService.listar(eventoId, page, size);
  }

  @PostMapping(
    value = "/eventos/{eventoId}/comentarios",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  public ResponseEntity<EventoCommentResponse> crear(
    @PathVariable Integer eventoId,
    @RequestPart(value = "contenido", required = false) String contenido,
    @RequestPart(value = "imagen", required = false) MultipartFile imagen,
    Authentication auth
  ) {
    String correo = auth.getName();
    CreateEventoCommentRequest request = new CreateEventoCommentRequest(contenido);
    return ResponseEntity.ok(eventoCommentService.crear(eventoId, request, imagen, correo));
  }

  @PutMapping("/comentarios/{commentId}")
  public ResponseEntity<EventoCommentResponse> actualizar(
    @PathVariable Integer commentId,
    @Valid @RequestBody UpdateEventoCommentRequest request,
    Authentication auth
  ) {
    String correo = auth.getName();
    return ResponseEntity.ok(eventoCommentService.actualizar(commentId, request, correo));
  }

  @DeleteMapping("/comentarios/{commentId}")
  @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
  public ResponseEntity<Void> eliminar(
    @PathVariable Integer commentId,
    Authentication auth
  ) {
    boolean esAdmin = auth.getAuthorities().stream()
      .anyMatch(a -> a.getAuthority().equals("ADMINISTRADOR"));

    eventoCommentService.eliminarComentarioEvento(
      commentId,
      auth.getName(),
      esAdmin
    );

    return ResponseEntity.noContent().build();
  }
}
