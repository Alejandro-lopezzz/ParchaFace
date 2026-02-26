package com.alejo.parchaface.dto;

import java.time.LocalDateTime;

public record EventoCommentResponse(
        Integer id,
        Integer eventoId,
        Integer usuarioId,
        String nombreUsuario,
        String contenido,
        LocalDateTime createdAt
) {}