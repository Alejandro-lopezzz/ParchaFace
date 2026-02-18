package com.alejo.parchaface.dto;

public record ClimaResponse(
  String ciudad,
  String pais,
  double lat,
  double lon,
  double temperaturaC,
  double vientoKmh,
  int codigoClima,
  String timezone
) {}
