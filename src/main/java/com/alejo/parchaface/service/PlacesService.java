package com.alejo.parchaface.service;

import com.alejo.parchaface.dto.RadarPlace;
import java.util.List;

public interface PlacesService {
  List<RadarPlace> buscarLugares(double lat, double lng, int rangoMetros);
}
