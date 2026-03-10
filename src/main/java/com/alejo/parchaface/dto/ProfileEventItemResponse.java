package com.alejo.parchaface.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProfileEventItemResponse(
  Integer idEvento,
  String titulo,
  String categoria,
  LocalDateTime fecha,
  String ciudad,
  String nombreLugar,
  String imagenPortadaUrl,
  String estadoEvento,
  String tipoRelacion,
  LocalDate fechaInscripcion
) {
}
