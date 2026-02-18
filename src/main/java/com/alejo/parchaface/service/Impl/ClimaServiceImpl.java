package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.dto.ClimaResponse;
import com.alejo.parchaface.service.ClimaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class ClimaServiceImpl implements ClimaService {

  private final RestClient restClient;

  public ClimaServiceImpl(RestClient.Builder builder) {
    this.restClient = builder.build();
  }

  @Override
  public ClimaResponse consultarClimaPorCiudad(String ciudad) {
    if (ciudad == null || ciudad.trim().length() < 2) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "La ciudad es obligatoria (mínimo 2 letras).");
    }

    String city = ciudad.trim();

    // 1) Geocoding: obtener lat/lon de una ciudad (solo Colombia)
    Map<?, ?> geo = restClient.get()
      .uri(uriBuilder -> uriBuilder
        .scheme("https")
        .host("geocoding-api.open-meteo.com")
        .path("/v1/search")
        .queryParam("name", city)
        .queryParam("count", 1)
        .queryParam("language", "es")
        .queryParam("countryCode", "CO")
        .build())
      .retrieve()
      .body(Map.class);

    List<Map<String, Object>> results = (List<Map<String, Object>>) geo.get("results");
    if (results == null || results.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
        "No encontré esa ciudad en Colombia.");
    }

    Map<String, Object> loc = results.get(0);
    double lat = ((Number) loc.get("latitude")).doubleValue();
    double lon = ((Number) loc.get("longitude")).doubleValue();

    String nombre = (String) loc.getOrDefault("name", city);
    String pais = (String) loc.getOrDefault("country", "Colombia");

    // 2) Forecast: clima actual con current=
    Map<?, ?> forecast = restClient.get()
      .uri(uriBuilder -> uriBuilder
        .scheme("https")
        .host("api.open-meteo.com")
        .path("/v1/forecast")
        .queryParam("latitude", lat)
        .queryParam("longitude", lon)
        .queryParam("current", "temperature_2m,weather_code,wind_speed_10m")
        .queryParam("temperature_unit", "celsius")
        .queryParam("wind_speed_unit", "kmh")
        .queryParam("timezone", "America/Bogota")
        .build())
      .retrieve()
      .body(Map.class);

    Map<String, Object> current = (Map<String, Object>) forecast.get("current");
    if (current == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
        "El proveedor no devolvió 'current'.");
    }

    double temp = ((Number) current.get("temperature_2m")).doubleValue();
    double wind = ((Number) current.get("wind_speed_10m")).doubleValue();
    int code = ((Number) current.get("weather_code")).intValue();

    return new ClimaResponse(
      nombre,
      pais,
      lat,
      lon,
      temp,
      wind,
      code,
      "America/Bogota"
    );
  }
}
