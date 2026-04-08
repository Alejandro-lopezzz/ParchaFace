package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.dto.CiudadSugerencia;
import com.alejo.parchaface.service.CiudadAutocompleteService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class CiudadAutocompleteServiceImpl implements CiudadAutocompleteService {

  private final RestClient restClient;

  public CiudadAutocompleteServiceImpl(RestClient.Builder builder) {
    this.restClient = builder.build();
  }

  @Override
  public List<CiudadSugerencia> sugerir(String query, int limit) {
    String q = (query == null) ? "" : query.trim();
    if (q.length() < 2) return List.of();

    Map<?, ?> geo = restClient.get()
      .uri(uriBuilder -> uriBuilder
        .scheme("https")
        .host("geocoding-api.open-meteo.com")
        .path("/v1/search")
        // pedimos más para poder rankear bien
        .queryParam("name", q)
        .queryParam("count", 20)
        .queryParam("language", "es")
        .queryParam("countryCode", "CO")
        .build())
      .retrieve()
      .body(Map.class);

    List<Map<String, Object>> results = (List<Map<String, Object>>) geo.get("results");
    if (results == null || results.isEmpty()) return List.of();

    // Rank + dedupe por nombre|admin1
    record Candidate(String name, String admin1, String featureCode, long population) {}

    List<Candidate> candidates = new ArrayList<>();
    Set<String> seen = new HashSet<>();

    for (Map<String, Object> r : results) {
      String name = String.valueOf(r.getOrDefault("name", ""));
      String admin1 = String.valueOf(r.getOrDefault("admin1", "")); // depto
      if (name.isBlank()) continue;

      String featureCode = String.valueOf(r.getOrDefault("feature_code", ""));
      long population = 0L;
      Object popObj = r.get("population");
      if (popObj instanceof Number n) population = n.longValue();

      String key = (name + "|" + admin1).toLowerCase();
      if (seen.add(key)) {
        candidates.add(new Candidate(name, admin1, featureCode, population));
      }
    }

    candidates.sort((a, b) -> {
      int aCap = "PPLC".equalsIgnoreCase(a.featureCode()) ? 1 : 0;
      int bCap = "PPLC".equalsIgnoreCase(b.featureCode()) ? 1 : 0;
      if (aCap != bCap) return Integer.compare(bCap, aCap); // capital primero

      int pop = Long.compare(b.population(), a.population()); // más poblado primero
      if (pop != 0) return pop;

      int dep = a.admin1().compareToIgnoreCase(b.admin1());
      if (dep != 0) return dep;

      return a.name().compareToIgnoreCase(b.name());
    });

    // limit final (lo que pidió el cliente)
    int finalLimit = Math.min(Math.max(limit, 5), 20);
    if (candidates.size() > finalLimit) candidates = candidates.subList(0, finalLimit);

    return candidates.stream()
      .map(c -> new CiudadSugerencia(c.name(), c.admin1()))
      .toList();
  }
}
