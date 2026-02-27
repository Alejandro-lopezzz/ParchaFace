package com.alejo.parchaface.service;

import com.alejo.parchaface.dto.CreateEventoCommentRequest;
import com.alejo.parchaface.dto.EventoCommentResponse;
import com.alejo.parchaface.dto.UpdateEventoCommentRequest;
import org.springframework.data.domain.Page;

public interface EventoCommentService {

    Page<EventoCommentResponse> listar(Integer eventoId, int page, int size);

    EventoCommentResponse crear(Integer eventoId, CreateEventoCommentRequest request, String correo);

    EventoCommentResponse actualizar(Integer commentId, UpdateEventoCommentRequest request, String correo);

    void eliminar(Integer commentId, String correo);
}