package com.alejo.parchaface.service;

import com.alejo.parchaface.dto.CiudadSugerencia;
import java.util.List;

public interface CiudadAutocompleteService {
  List<CiudadSugerencia> sugerir(String query, int limit);
}
