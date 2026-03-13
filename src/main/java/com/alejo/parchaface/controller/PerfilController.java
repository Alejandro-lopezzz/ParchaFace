package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.ProfileActivityItemResponse;
import com.alejo.parchaface.dto.ProfileEventItemResponse;
import com.alejo.parchaface.model.*;
import com.alejo.parchaface.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/perfil")
public class PerfilController {

  private final UsuarioRepository usuarioRepository;
  private final EventoRepository eventoRepository;
  private final InscripcionRepository inscripcionRepository;
  private final CommunityPostRepository communityPostRepository;
  private final CommunityCommentRepository communityCommentRepository;
  private final EventoCommentRepository eventoCommentRepository;
  private final NotificacionRepository notificacionRepository;

  public PerfilController(
    UsuarioRepository usuarioRepository,
    EventoRepository eventoRepository,
    InscripcionRepository inscripcionRepository,
    CommunityPostRepository communityPostRepository,
    CommunityCommentRepository communityCommentRepository,
    EventoCommentRepository eventoCommentRepository,
    NotificacionRepository notificacionRepository
  ) {
    this.usuarioRepository = usuarioRepository;
    this.eventoRepository = eventoRepository;
    this.inscripcionRepository = inscripcionRepository;
    this.communityPostRepository = communityPostRepository;
    this.communityCommentRepository = communityCommentRepository;
    this.eventoCommentRepository = eventoCommentRepository;
    this.notificacionRepository = notificacionRepository;
  }

  @GetMapping("/mis-eventos-creados")
  @Transactional(readOnly = true)
  public List<ProfileEventItemResponse> misEventosCreados(Principal principal) {
    Usuario usuario = getUsuarioActual(principal);

    return eventoRepository.findByOrganizador_IdUsuarioOrderByFechaCreacionDesc(usuario.getIdUsuario())
      .stream()
      .map(evento -> toProfileEvent(evento, "CREADO_POR_MI", null))
      .toList();
  }

  @GetMapping("/mis-eventos-inscritos")
  @Transactional(readOnly = true)
  public List<ProfileEventItemResponse> misEventosInscritos(Principal principal) {
    Usuario usuario = getUsuarioActual(principal);

    return inscripcionRepository.findByUsuario_IdUsuarioOrderByFechaInscripcionDesc(usuario.getIdUsuario())
      .stream()
      .map(inscripcion -> toProfileEvent(
        inscripcion.getEvento(),
        "INSCRITO",
        inscripcion.getFechaInscripcion()
      ))
      .toList();
  }

  @GetMapping("/actividad")
  @Transactional(readOnly = true)
  public List<ProfileActivityItemResponse> miActividad(Principal principal) {
    Usuario usuario = getUsuarioActual(principal);

    List<ProfileActivityItemResponse> items = new ArrayList<>();

    for (Evento evento : eventoRepository.findByOrganizador_IdUsuarioOrderByFechaCreacionDesc(usuario.getIdUsuario())) {
      items.add(new ProfileActivityItemResponse(
        "EVENTO_CREADO",
        "Creaste un evento",
        "Creaste el evento \"" + safe(evento.getTitulo()) + "\"",
        firstNonNull(evento.getFechaCreacion(), evento.getFecha()),
        evento.getIdEvento(),
        "EVENTO"
      ));
    }

    for (Inscripcion inscripcion : inscripcionRepository.findByUsuario_IdUsuarioOrderByFechaInscripcionDesc(usuario.getIdUsuario())) {
      Evento evento = inscripcion.getEvento();

      items.add(new ProfileActivityItemResponse(
        "INSCRIPCION_EVENTO",
        "Te inscribiste a un evento",
        "Te inscribiste al evento \"" + safe(evento.getTitulo()) + "\"",
        atStartOfDay(inscripcion.getFechaInscripcion()),
        evento.getIdEvento(),
        "EVENTO"
      ));
    }

    for (CommunityPost post : communityPostRepository.findByUsuario_IdUsuarioOrderByCreatedAtDesc(usuario.getIdUsuario())) {
      items.add(new ProfileActivityItemResponse(
        "POST_COMUNIDAD",
        "Publicaste en la comunidad",
        "Publicaste \"" + safe(post.getTitle()) + "\" en comunidad",
        post.getCreatedAt(),
        post.getIdPost(),
        "COMMUNITY_POST"
      ));
    }

    for (CommunityComment comment : communityCommentRepository.findByUsuario_IdUsuarioOrderByCreatedAtDesc(usuario.getIdUsuario())) {
      items.add(new ProfileActivityItemResponse(
        "COMENTARIO_COMUNIDAD",
        "Comentaste en la comunidad",
        resumir(comment.getContent(), 110),
        comment.getCreatedAt(),
        comment.getPostId(),
        "COMMUNITY_POST"
      ));
    }

    for (EventoComment comment : eventoCommentRepository.findByUsuario_IdUsuarioOrderByCreatedAtDesc(usuario.getIdUsuario())) {
      String tituloEvento = comment.getEvento() != null ? safe(comment.getEvento().getTitulo()) : "evento";
      Integer eventoId = comment.getEvento() != null ? comment.getEvento().getIdEvento() : null;

      items.add(new ProfileActivityItemResponse(
        "COMENTARIO_EVENTO",
        "Comentaste en un evento",
        "Comentaste en \"" + tituloEvento + "\": " + resumir(comment.getContenido(), 110),
        comment.getCreatedAt(),
        eventoId,
        "EVENTO"
      ));
    }

    for (Notificacion notificacion : notificacionRepository.findByUsuario_IdUsuarioOrderByFechaEnvioDesc(usuario.getIdUsuario())) {
      items.add(new ProfileActivityItemResponse(
        "NOTIFICACION",
        "Recibiste una notificación",
        safe(notificacion.getMensaje()),
        notificacion.getFechaEnvio(),
        notificacion.getId_notificacion(),
        "NOTIFICACION"
      ));
    }

    items.sort(Comparator.comparing(
      ProfileActivityItemResponse::fecha,
      Comparator.nullsLast(Comparator.reverseOrder())
    ));

    return items;
  }

  private Usuario getUsuarioActual(Principal principal) {
    if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
    }

    return usuarioRepository.findByCorreo(principal.getName())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
  }

  private ProfileEventItemResponse toProfileEvent(Evento evento, String tipoRelacion, LocalDate fechaInscripcion) {
    return new ProfileEventItemResponse(
      evento.getIdEvento(),
      evento.getTitulo(),
      evento.getCategoria(),
      evento.getFecha(),
      evento.getCiudad(),
      evento.getNombreLugar(),
      evento.getImagenPortadaUrl(),
      evento.getEstadoEvento() != null ? evento.getEstadoEvento().name() : null,
      tipoRelacion,
      fechaInscripcion
    );
  }

  private LocalDateTime atStartOfDay(LocalDate date) {
    return date != null ? date.atStartOfDay() : null;
  }

  private LocalDateTime firstNonNull(LocalDateTime a, LocalDateTime b) {
    return a != null ? a : b;
  }

  private String resumir(String text, int max) {
    String clean = safe(text).trim();
    if (clean.length() <= max) return clean;
    return clean.substring(0, max).trim() + "...";
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }
}
