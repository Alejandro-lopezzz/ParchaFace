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

@Service
public class EventoCommentServiceImpl implements EventoCommentService {

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
    public EventoCommentResponse crear(Integer eventoId, CreateEventoCommentRequest request, String correo) {
        Evento evento = eventoRepo.findById(eventoId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        if (Boolean.FALSE.equals(evento.getPermitirComentarios())) {
            throw new RuntimeException("Este evento no permite comentarios");
        }

        Usuario usuario = usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        EventoComment c = new EventoComment();
        c.setEvento(evento);
        c.setUsuario(usuario);
        c.setContenido(request.contenido());

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

    private EventoCommentResponse toResponse(EventoComment c) {
        return new EventoCommentResponse(
                c.getIdEventoComment(),
                c.getEvento().getIdEvento(),
                c.getUsuario().getIdUsuario(),
                c.getUsuario().getNombre(),
                c.getContenido(),
                c.getCreatedAt()
        );
    }
}