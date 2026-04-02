package com.alejo.parchaface.controller;

import com.alejo.parchaface.model.CommunityComment;
import com.alejo.parchaface.model.CommunityPost;
import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.service.AdminModerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ADMINISTRADOR')")
public class AdminController {

  private final AdminModerationService adminModerationService;

  public AdminController(AdminModerationService adminModerationService) {
    this.adminModerationService = adminModerationService;
  }

  @GetMapping("/eventos/pendientes")
  public List<Evento> listarEventosPendientes() {
    return adminModerationService.listarEventosPendientes();
  }

  @PutMapping("/eventos/{id}/aprobar")
  public ResponseEntity<?> aprobarEvento(@PathVariable Integer id) {
    Evento evento = adminModerationService.aprobarEvento(id);
    return ResponseEntity.ok(Map.of("mensaje", "Evento aprobado", "evento", evento));
  }

  @PutMapping("/eventos/{id}/rechazar")
  public ResponseEntity<?> rechazarEvento(@PathVariable Integer id, @RequestParam(required = false) String motivo) {
    Evento evento = adminModerationService.rechazarEvento(id, motivo);
    return ResponseEntity.ok(Map.of("mensaje", "Evento rechazado", "evento", evento));
  }

  @GetMapping("/usuarios")
  public List<Usuario> listarUsuarios() {
    return adminModerationService.listarUsuarios();
  }

  @PutMapping("/usuarios/{id}/suspender")
  public Usuario suspenderUsuario(@PathVariable Integer id) {
    return adminModerationService.suspenderUsuario(id);
  }

  @PutMapping("/usuarios/{id}/activar")
  public Usuario activarUsuario(@PathVariable Integer id) {
    return adminModerationService.activarUsuario(id);
  }

  @DeleteMapping("/usuarios/{id}")
  public ResponseEntity<Void> eliminarUsuario(@PathVariable Integer id) {
    adminModerationService.eliminarUsuario(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/community/posts")
  public List<CommunityPost> listarPosts() {
    return adminModerationService.listarPosts();
  }

  @DeleteMapping("/community/posts/{id}")
  public ResponseEntity<Void> eliminarPost(@PathVariable Integer id) {
    adminModerationService.eliminarPost(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/community/comments")
  public List<CommunityComment> listarComentarios() {
    return adminModerationService.listarComentarios();
  }

  @DeleteMapping("/community/comments/{id}")
  public ResponseEntity<Void> eliminarComentario(@PathVariable Integer id) {
    adminModerationService.eliminarComentario(id);
    return ResponseEntity.noContent().build();
  }
}
