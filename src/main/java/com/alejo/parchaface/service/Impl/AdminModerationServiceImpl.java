package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.model.CommunityComment;
import com.alejo.parchaface.model.CommunityPost;
import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.Estado;
import com.alejo.parchaface.model.enums.Rol;
import com.alejo.parchaface.repository.CommentLikeRepository;
import com.alejo.parchaface.repository.CommunityCommentRepository;
import com.alejo.parchaface.repository.CommunityPostRepository;
import com.alejo.parchaface.repository.EventoCommentRepository;
import com.alejo.parchaface.repository.EventoRepository;
import com.alejo.parchaface.repository.InscripcionRepository;
import com.alejo.parchaface.repository.NotificacionRepository;
import com.alejo.parchaface.repository.PostRatingRepository;
import com.alejo.parchaface.repository.SeguimientoRepository;
import com.alejo.parchaface.repository.UsuarioRepository;
import com.alejo.parchaface.service.AdminModerationService;
import com.alejo.parchaface.service.EventoService;
import com.alejo.parchaface.service.NotificacionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminModerationServiceImpl implements AdminModerationService {

  private final EventoService eventoService;
  private final UsuarioRepository usuarioRepository;
  private final CommunityPostRepository communityPostRepository;
  private final CommunityCommentRepository communityCommentRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final PostRatingRepository postRatingRepository;
  private final NotificacionService notificacionService;

  private final EventoRepository eventoRepository;
  private final InscripcionRepository inscripcionRepository;
  private final NotificacionRepository notificacionRepository;
  private final EventoCommentRepository eventoCommentRepository;
  private final SeguimientoRepository seguimientoRepository;

  private final String adminCorreoPrincipal;

  public AdminModerationServiceImpl(
    EventoService eventoService,
    UsuarioRepository usuarioRepository,
    CommunityPostRepository communityPostRepository,
    CommunityCommentRepository communityCommentRepository,
    CommentLikeRepository commentLikeRepository,
    PostRatingRepository postRatingRepository,
    NotificacionService notificacionService,
    EventoRepository eventoRepository,
    InscripcionRepository inscripcionRepository,
    NotificacionRepository notificacionRepository,
    EventoCommentRepository eventoCommentRepository,
    SeguimientoRepository seguimientoRepository,
    @Value("${app.admin.correo:admin@parchaface.com}") String adminCorreoPrincipal
  ) {
    this.eventoService = eventoService;
    this.usuarioRepository = usuarioRepository;
    this.communityPostRepository = communityPostRepository;
    this.communityCommentRepository = communityCommentRepository;
    this.commentLikeRepository = commentLikeRepository;
    this.postRatingRepository = postRatingRepository;
    this.notificacionService = notificacionService;
    this.eventoRepository = eventoRepository;
    this.inscripcionRepository = inscripcionRepository;
    this.notificacionRepository = notificacionRepository;
    this.eventoCommentRepository = eventoCommentRepository;
    this.seguimientoRepository = seguimientoRepository;
    this.adminCorreoPrincipal = adminCorreoPrincipal.trim().toLowerCase();
  }

  @Override
  public List<Evento> listarEventosPendientes() {
    return eventoService.listarPendientesAprobacion();
  }

  @Override
  public Evento aprobarEvento(Integer idEvento) {
    return eventoService.aprobarEvento(idEvento);
  }

  @Override
  public Evento rechazarEvento(Integer idEvento, String motivo) {
    return eventoService.rechazarEvento(idEvento, motivo);
  }

  @Override
  public List<Usuario> listarUsuarios() {
    return usuarioRepository.findAll();
  }

  @Override
  @Transactional
  public Usuario suspenderUsuario(Integer idUsuario) {
    Usuario usuario = buscarUsuario(idUsuario);
    validarUsuarioAdministrable(usuario);
    usuario.setEstado(Estado.SUSPENDIDO);
    Usuario guardado = usuarioRepository.save(usuario);

    notificacionService.crearNotificacionConReferencia(
      guardado,
      "Tu cuenta fue suspendida por el administrador.",
      "USUARIO_SUSPENDIDO",
      guardado.getIdUsuario()
    );

    return guardado;
  }

  @Override
  @Transactional
  public Usuario activarUsuario(Integer idUsuario) {
    Usuario usuario = buscarUsuario(idUsuario);
    usuario.setEstado(Estado.ACTIVO);
    Usuario guardado = usuarioRepository.save(usuario);

    notificacionService.crearNotificacionConReferencia(
      guardado,
      "Tu cuenta fue reactivada por el administrador.",
      "USUARIO_ACTIVADO",
      guardado.getIdUsuario()
    );

    return guardado;
  }

  @Override
  @Transactional
  public void eliminarUsuario(Integer idUsuario) {
    Usuario usuario = buscarUsuario(idUsuario);
    validarUsuarioAdministrable(usuario);

    // comentarios de comunidad creados por el usuario
    List<CommunityComment> comentariosUsuario =
      communityCommentRepository.findByUsuario_IdUsuarioOrderByCreatedAtDesc(idUsuario);

    for (CommunityComment comment : comentariosUsuario) {
      commentLikeRepository.deleteByCommentId(comment.getIdComment());
      communityCommentRepository.delete(comment);
    }

    // posts de comunidad creados por el usuario
    List<CommunityPost> posts =
      communityPostRepository.findByUsuario_IdUsuarioOrderByCreatedAtDesc(idUsuario);

    for (CommunityPost post : posts) {
      List<CommunityComment> comentariosPost =
        communityCommentRepository.findByPostIdOrderByCreatedAtAsc(post.getIdPost());

      for (CommunityComment comment : comentariosPost) {
        commentLikeRepository.deleteByCommentId(comment.getIdComment());
      }

      communityCommentRepository.deleteByPostId(post.getIdPost());
      postRatingRepository.deleteByPostId(post.getIdPost());
      communityPostRepository.delete(post);
    }

    // inscripciones y notificaciones del usuario
    inscripcionRepository.deleteByUsuario_IdUsuario(idUsuario);
    notificacionRepository.deleteByUsuario_IdUsuario(idUsuario);

    // eventos creados por el usuario
    List<Evento> eventos =
      eventoRepository.findByOrganizador_IdUsuarioOrderByFechaCreacionDesc(idUsuario);

    for (Evento evento : eventos) {
      eventoCommentRepository.deleteByEvento_IdEvento(evento.getIdEvento());
      inscripcionRepository.deleteByEvento_IdEvento(evento.getIdEvento());
      eventoRepository.delete(evento);
    }

    // relaciones de seguimiento
    seguimientoRepository.deleteBySeguidor(usuario);
    seguimientoRepository.deleteBySeguido(usuario);

    // borrar usuario físicamente
    usuarioRepository.delete(usuario);
  }

  @Override
  public List<CommunityPost> listarPosts() {
    return communityPostRepository.findAllByOrderByCreatedAtDesc();
  }

  @Override
  public List<CommunityComment> listarComentarios() {
    return communityCommentRepository.findAllByOrderByCreatedAtDesc();
  }

  @Override
  @Transactional
  public void eliminarPost(Integer idPost) {
    CommunityPost post = communityPostRepository.findById(idPost)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Discusión no encontrada"));

    List<CommunityComment> comments = communityCommentRepository.findByPostIdOrderByCreatedAtAsc(idPost);
    for (CommunityComment comment : comments) {
      commentLikeRepository.deleteByCommentId(comment.getIdComment());
    }

    communityCommentRepository.deleteByPostId(idPost);
    postRatingRepository.deleteByPostId(idPost);
    communityPostRepository.delete(post);
  }

  @Override
  @Transactional
  public void eliminarComentario(Integer idComentario) {
    CommunityComment comment = communityCommentRepository.findById(idComentario)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentario no encontrado"));

    commentLikeRepository.deleteByCommentId(idComentario);
    communityCommentRepository.delete(comment);

    communityPostRepository.findById(comment.getPostId()).ifPresent(post -> {
      long total = communityCommentRepository.countByPostId(comment.getPostId());
      post.setCommentsCount((int) total);
      communityPostRepository.save(post);
    });
  }

  private Usuario buscarUsuario(Integer idUsuario) {
    return usuarioRepository.findById(idUsuario)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
  }

  private void validarUsuarioAdministrable(Usuario usuario) {
    String correo = usuario.getCorreo() == null ? "" : usuario.getCorreo().trim().toLowerCase();

    if (usuario.getRol() == Rol.ADMINISTRADOR || correo.equals(adminCorreoPrincipal)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes modificar el administrador principal");
    }
  }
}
