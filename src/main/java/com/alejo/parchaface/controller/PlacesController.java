package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.RadarPlace;
import com.alejo.parchaface.service.PlacesService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/places")
public class PlacesController {

  private final PlacesService placesService;

  public PlacesController(PlacesService placesService) {
    this.placesService = placesService;
  }

  @GetMapping
  public List<RadarPlace> places(
    @RequestParam double lat,
    @RequestParam double lng,
    @RequestParam(name = "rango", required = false) Integer rango
  ) {
    int r = (rango != null) ? rango : 1500;
    return placesService.buscarLugares(lat, lng, r);
  }
}
