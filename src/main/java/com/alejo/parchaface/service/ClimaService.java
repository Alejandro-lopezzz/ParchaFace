package com.alejo.parchaface.service;

import com.alejo.parchaface.dto.ClimaResponse;

public interface ClimaService {
  ClimaResponse consultarClimaPorCiudad(String ciudad);
}
