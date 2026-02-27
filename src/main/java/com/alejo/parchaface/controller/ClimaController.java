package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.ClimaResponse;
import com.alejo.parchaface.service.ClimaService;
import org.springframework.web.bind.annotation.*;
import com.alejo.parchaface.dto.CiudadSugerencia;
import com.alejo.parchaface.service.CiudadAutocompleteService;

import java.util.List;

@RestController
@RequestMapping("/api/clima")
public class ClimaController {

  private final ClimaService climaService;
  private final CiudadAutocompleteService ciudadAutocompleteService;

  public ClimaController(ClimaService climaService,
                         CiudadAutocompleteService ciudadAutocompleteService) {
    this.climaService = climaService;
    this.ciudadAutocompleteService = ciudadAutocompleteService;
  }

  // GET /api/clima?ciudad=Bogota
  @GetMapping
  public ClimaResponse getClima(@RequestParam String ciudad) {
    return climaService.consultarClimaPorCiudad(ciudad);
  }

  @GetMapping("/ciudades")
  public List<CiudadSugerencia> ciudades(
    @RequestParam(name = "query", required = false) String query
  ) {
    return ciudadAutocompleteService.sugerir(query, 20);
  }
}
