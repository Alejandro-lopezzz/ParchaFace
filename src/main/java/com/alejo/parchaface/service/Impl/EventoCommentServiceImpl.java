package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.dto.CreateEventoCommentRequest;
import com.alejo.parchaface.dto.EventoCommentResponse;
import com.alejo.parchaface.dto.UpdateEventoCommentRequest;
import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.EventoComment;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.repository.EventoCommentRepository;
import com.alejo.parchaface.repository.EventoRepository;
import com.alejo.parchaface.repository.UsuarioRepository;
import com.alejo.parchaface.service.EventoCommentService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@Service
public class EventoCommentServiceImpl implements EventoCommentService {

  private static final String CLOUDINARY_FOLDER_COMENTARIOS = "parchaface/comentarios";

  private final EventoCommentRepository commentRepo;
  private final EventoRepository eventoRepo;
  private final UsuarioRepository usuarioRepo;
  private final Cloudinary cloudinary;

  public EventoCommentServiceImpl(
    EventoCommentRepository commentRepo,
    EventoRepository eventoRepo,
    UsuarioRepository usuarioRepo,
    Cloudinary cloudinary
  ) {
    this.commentRepo = commentRepo;
    this.eventoRepo = eventoRepo;
    this.usuarioRepo = usuarioRepo;
    this.cloudinary = cloudinary;
  }

  @Override
  public Page<EventoCommentResponse> listar(Integer eventoId, int page, int size) {
    return commentRepo
      .findByEvento_IdEventoOrderByCreatedAtDesc(eventoId, PageRequest.of(page, size))
      .map(this::toResponse);
  }

  @Override
  @Transactional
  public EventoCommentResponse crear(Integer eventoId, CreateEventoCommentRequest request, MultipartFile imagen, String correo) {
    Evento evento = eventoRepo.findById(eventoId)
      .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

    if (Boolean.FALSE.equals(evento.getPermitirComentarios())) {
      throw new RuntimeException("Este evento no permite comentarios");
    }

    Usuario usuario = usuarioRepo.findByCorreo(correo)
      .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

    String contenido = request.contenido() == null ? "" : request.contenido().trim();
    boolean tieneImagen = imagen != null && !imagen.isEmpty();

    if (contenido.isBlank() && !tieneImagen) {
      throw new RuntimeException("Debes escribir un comentario o adjuntar una imagen");
    }

    EventoComment comment = new EventoComment();
    comment.setEvento(evento);
    comment.setUsuario(usuario);
    comment.setContenido(contenido);

    String publicIdSubido = null;

    try {
      if (tieneImagen) {
        ImagenCloudinarySubida subida = subirImagenACloudinary(imagen);
        publicIdSubido = subida.publicId();

        comment.setImagenUrl(subida.secureUrl());
        comment.setImagenPublicId(subida.publicId());
      }

      return toResponse(commentRepo.save(comment));

    } catch (RuntimeException e) {
      if (StringUtils.hasText(publicIdSubido)) {
        eliminarImagenDeCloudinary(publicIdSubido);
      }
      throw e;
    }
  }

  @Override
  @Transactional
  public EventoCommentResponse actualizar(Integer commentId, UpdateEventoCommentRequest request, String correo) {
    Usuario usuario = usuarioRepo.findByCorreo(correo)
      .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

    EventoComment comment = commentRepo.findByIdEventoCommentAndUsuario_IdUsuario(commentId, usuario.getIdUsuario())
      .orElseThrow(() -> new RuntimeException("Comentario no existe o no es tuyo"));

    String contenido = request.contenido() == null ? "" : request.contenido().trim();

    if (contenido.isBlank() && !StringUtils.hasText(comment.getImagenUrl())) {
      throw new RuntimeException("El comentario no puede quedar vacío");
    }

    comment.setContenido(contenido);
    return toResponse(commentRepo.save(comment));
  }

  @Override
  @Transactional
  public void eliminar(Integer commentId, String correo) {
    eliminarComentarioEvento(commentId, correo, false);
  }

  private ImagenCloudinarySubida subirImagenACloudinary(MultipartFile file) {
    validarTipoImagen(file);

    try {
      Map<?, ?> resultado = cloudinary.uploader().upload(
        file.getBytes(),
        ObjectUtils.asMap(
          "folder", CLOUDINARY_FOLDER_COMENTARIOS,
          "resource_type", "image"
        )
      );

      String secureUrl = (String) resultado.get("secure_url");
      String publicId = (String) resultado.get("public_id");

      if (!StringUtils.hasText(secureUrl) || !StringUtils.hasText(publicId)) {
        throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Cloudinary no devolvió una URL o public_id válidos"
        );
      }

      return new ImagenCloudinarySubida(secureUrl, publicId);

    } catch (IOException e) {
      throw new ResponseStatusException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "No se pudo subir la imagen del comentario"
      );
    }
  }

  private void validarTipoImagen(MultipartFile file) {
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase().trim();

    boolean permitido =
      contentType.equals("image/jpeg") ||
        contentType.equals("image/jpg") ||
        contentType.equals("image/png") ||
        contentType.equals("image/webp");

    if (!permitido) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Tipo de imagen no permitido: " + contentType
      );
    }
  }

  private void eliminarImagenDeCloudinary(String publicId) {
    if (!StringUtils.hasText(publicId)) {
      return;
    }

    try {
      cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    } catch (Exception ignored) {
      // No bloquea el flujo si falla el borrado remoto
    }
  }

  private EventoCommentResponse toResponse(EventoComment c) {
    return new EventoCommentResponse(
      c.getIdEventoComment(),
      c.getEvento().getIdEvento(),
      c.getUsuario().getIdUsuario(),
      c.getUsuario().getNombre(),
      c.getContenido(),
      c.getImagenUrl(),
      c.getCreatedAt()
    );
  }

  private record ImagenCloudinarySubida(String secureUrl, String publicId) {
  }

  @Override
  @Transactional
  public void eliminarComentarioEvento(Integer idComentario, String correo, boolean esAdmin) {
    EventoComment comentario = commentRepo.findById(idComentario)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentario no encontrado"));

    String correoAutor = comentario.getUsuario() != null
      ? comentario.getUsuario().getCorreo()
      : null;

    if (!esAdmin && (correoAutor == null || !correoAutor.equalsIgnoreCase(correo))) {
      throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "No tienes permiso para eliminar este comentario"
      );
    }

    String publicId = comentario.getImagenPublicId();

    commentRepo.delete(comentario);

    if (StringUtils.hasText(publicId)) {
      eliminarImagenDeCloudinary(publicId);
    }
  }
}
