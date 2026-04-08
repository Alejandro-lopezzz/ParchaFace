package com.alejo.parchaface.dto;

import java.time.LocalDateTime;

public record AdminEventoCommentResponse(
  Integer idComment,
  Integer eventoId,
  String tituloEvento,
  Integer usuarioId,
  String nombreUsuario,
  String correoUsuario,
  String contenido,
  String imagenUrl,
  LocalDateTime createdAt
) {
}
