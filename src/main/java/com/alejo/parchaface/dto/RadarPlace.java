package com.alejo.parchaface.dto;

import java.util.List;

public record RadarPlace(
  String id,
  String categoria,
  String nombre,
  RadarLatLng ubicacion,
  Double rating,
  Boolean open_now,
  List<String> horarios
) {}
