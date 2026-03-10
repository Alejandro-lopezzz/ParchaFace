package com.alejo.parchaface.dto;

import java.time.LocalDateTime;

public record ProfileActivityItemResponse(
  String tipo,
  String titulo,
  String descripcion,
  LocalDateTime fecha,
  Integer referenciaId,
  String referenciaTipo
) {
}
