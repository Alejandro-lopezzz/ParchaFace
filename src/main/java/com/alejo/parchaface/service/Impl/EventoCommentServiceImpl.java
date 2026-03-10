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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class EventoCommentServiceImpl implements EventoCommentService {

    private static final Path UPLOAD_DIR = Paths.get("uploads", "comentarios");
    private static final String PUBLIC_URL_PREFIX = "/uploads/comentarios/";

    private final EventoCommentRepository commentRepo;
    private final EventoRepository eventoRepo;
    private final UsuarioRepository usuarioRepo;

    public EventoCommentServiceImpl(EventoCommentRepository commentRepo,
                                    EventoRepository eventoRepo,
                                    UsuarioRepository usuarioRepo) {
        this.commentRepo = commentRepo;
        this.eventoRepo = eventoRepo;
        this.usuarioRepo = usuarioRepo;
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

        EventoComment c = new EventoComment();
        c.setEvento(evento);
        c.setUsuario(usuario);
        c.setContenido(contenido);

        if (tieneImagen) {
            c.setImagenUrl(saveCommentImageToDisk(imagen).publicUrl());
        }

        return toResponse(commentRepo.save(c));
    }

    @Override
    @Transactional
    public EventoCommentResponse actualizar(Integer commentId, UpdateEventoCommentRequest request, String correo) {
        Usuario usuario = usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        EventoComment c = commentRepo.findByIdEventoCommentAndUsuario_IdUsuario(commentId, usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Comentario no existe o no es tuyo"));

        c.setContenido(request.contenido());
        return toResponse(commentRepo.save(c));
    }

    @Override
    @Transactional
    public void eliminar(Integer commentId, String correo) {
        Usuario usuario = usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        EventoComment c = commentRepo.findByIdEventoCommentAndUsuario_IdUsuario(commentId, usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Comentario no existe o no es tuyo"));

        commentRepo.delete(c);
    }

    private SavedImage saveCommentImageToDisk(MultipartFile file) {
        try {
            Files.createDirectories(UPLOAD_DIR);

            String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase().trim();
            boolean ok = contentType.equals("image/jpeg")
                    || contentType.equals("image/jpg")
                    || contentType.equals("image/png")
                    || contentType.equals("image/webp");

            if (!ok) {
                throw new IllegalArgumentException("Tipo de imagen no permitido: " + contentType);
            }

            String originalName = StringUtils.cleanPath(
                    file.getOriginalFilename() == null ? "" : file.getOriginalFilename()
            );

            String ext = getExtension(originalName);

            if (ext.isBlank()) {
                ext = switch (contentType) {
                    case "image/png" -> "png";
                    case "image/webp" -> "webp";
                    default -> "jpg";
                };
            }

            String filename = UUID.randomUUID() + "." + ext;
            Path target = UPLOAD_DIR.resolve(filename).normalize();

            if (!target.startsWith(UPLOAD_DIR.normalize())) {
                throw new IllegalArgumentException("Ruta inválida para guardar archivo");
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return new SavedImage(PUBLIC_URL_PREFIX + filename);

        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la imagen del comentario", e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1).trim().toLowerCase();
    }

    private record SavedImage(String publicUrl) {}

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
}