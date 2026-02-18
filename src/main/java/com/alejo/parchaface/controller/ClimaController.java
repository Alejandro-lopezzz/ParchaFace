package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.ClimaResponse;
import com.alejo.parchaface.service.ClimaService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clima")
public class ClimaController {

  private final ClimaService climaService;

  public ClimaController(ClimaService climaService) {
    this.climaService = climaService;
  }

  // GET /api/clima?ciudad=Bogota
  @GetMapping
  public ClimaResponse getClima(@RequestParam String ciudad) {
    return climaService.consultarClimaPorCiudad(ciudad);
  }
}
