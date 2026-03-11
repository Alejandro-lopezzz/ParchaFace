package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.dto.RadarLatLng;
import com.alejo.parchaface.dto.RadarPlace;
import com.alejo.parchaface.service.PlacesService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeoapifyPlacesServiceImpl implements PlacesService {

  private final RestClient restClient;
  private final String apiKey;

  public GeoapifyPlacesServiceImpl(
    RestClient.Builder builder,
    @Value("${geoapify.api-key:}") String apiKey
  ) {
    this.restClient = builder.build();
    this.apiKey = apiKey;
  }

  @Override
  public List<RadarPlace> buscarLugares(double lat, double lng, int rangoMetros) {
    validar(lat, lng, rangoMetros);

    // dedupe por place_id
    Map<String, RadarPlace> byId = new LinkedHashMap<>();

    // Discotecas
    merge(byId, buscarPorCategoria("adult.nightclub", "discoteca", lat, lng, rangoMetros));

    // Bares
    merge(byId, buscarPorCategoria("catering.bar", "bar", lat, lng, rangoMetros));

    // Restaurantes
    merge(byId, buscarPorCategoria("catering.restaurant", "restaurante", lat, lng, rangoMetros));

    // Canchas (multi-categoría en una sola llamada está soportado) :contentReference[oaicite:13]{index=13}
    merge(byId, buscarPorCategoria("sport.pitch,sport.sports_centre,sport.stadium", "cancha", lat, lng, rangoMetros));

    return new ArrayList<>(byId.values());
  }

  private void validar(double lat, double lng, int rango) {
    if (lat < -90 || lat > 90) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lat fuera de rango.");
    if (lng < -180 || lng > 180) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lng fuera de rango.");
    if (rango < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rango debe ser >= 1.");
    if (apiKey == null || apiKey.isBlank()) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falta GEOAPIFY_API_KEY.");
    }
  }

  private void merge(Map<String, RadarPlace> byId, List<RadarPlace> incoming) {
    for (RadarPlace p : incoming) {
      if (p == null || p.id() == null) continue;
      byId.putIfAbsent(p.id(), p);
    }
  }

  private List<RadarPlace> buscarPorCategoria(
    String categories,
    String categoriaRadar,
    double lat,
    double lng,
    int rangoMetros
  ) {
    // Places API: https://api.geoapify.com/v2/places?PARAMS :contentReference[oaicite:14]{index=14}
    Map<?, ?> resp = restClient.get()
      .uri(uriBuilder -> uriBuilder
        .scheme("https")
        .host("api.geoapify.com")
        .path("/v2/places")
        .queryParam("categories", categories)
        .queryParam("filter", "circle:" + lng + "," + lat + "," + rangoMetros)     // circle:lon,lat,radius :contentReference[oaicite:15]{index=15}
        .queryParam("bias", "proximity:" + lng + "," + lat)                        // ordenar por cercanía :contentReference[oaicite:16]{index=16}
        .queryParam("limit", 20)
        .queryParam("apiKey", apiKey)
        .build())
      .retrieve()
      .body(Map.class);

    if (resp == null) return List.of();

    List<Map<String, Object>> features = (List<Map<String, Object>>) resp.get("features");
    if (features == null || features.isEmpty()) return List.of();

    List<RadarPlace> out = new ArrayList<>();

    for (Map<String, Object> f : features) {
      Map<String, Object> props = (Map<String, Object>) f.get("properties");
      if (props == null) continue;

      String placeId = asString(props.get("place_id")); // id para Place Details :contentReference[oaicite:17]{index=17}
      String name = asString(props.get("name"));
      Double plat = asDouble(props.get("lat"));
      Double plon = asDouble(props.get("lon"));

      if (placeId == null || name == null || plat == null || plon == null) continue;

      // Place Details para traer opening_hours (si existe) :contentReference[oaicite:18]{index=18}
      PlaceDetails details = getDetails(placeId);

      // rating: Geoapify no es Google; normalmente queda null (o a veces stars) :contentReference[oaicite:19]{index=19}
      Double rating = details.stars;

      List<String> horarios = details.openingHours == null
        ? List.of()
        : Arrays.stream(details.openingHours.split(";"))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toList();

      Boolean openNow = computeOpenNowBestEffort(details.openingHours);

      out.add(new RadarPlace(
        placeId,
        categoriaRadar,
        name,
        new RadarLatLng(plat, plon),
        rating,
        openNow,
        horarios
      ));
    }

    return out;
  }

  private PlaceDetails getDetails(String placeId) {
    Map<?, ?> resp = restClient.get()
      .uri(uriBuilder -> uriBuilder
        .scheme("https")
        .host("api.geoapify.com")
        .path("/v2/place-details")
        .queryParam("id", placeId)
        .queryParam("features", "details")
        .queryParam("apiKey", apiKey)
        .build())
      .retrieve()
      .body(Map.class);

    if (resp == null) return new PlaceDetails(null, null);

    List<Map<String, Object>> features = (List<Map<String, Object>>) resp.get("features");
    if (features == null || features.isEmpty()) return new PlaceDetails(null, null);

    Map<String, Object> props = (Map<String, Object>) features.get(0).get("properties");
    if (props == null) return new PlaceDetails(null, null);

    String openingHours = asString(props.get("opening_hours")); // :contentReference[oaicite:20]{index=20}

    // stars (si vienen)
    Double stars = null;
    Object cateringObj = props.get("catering");
    if (cateringObj instanceof Map<?, ?> catering) {
      Object s = catering.get("stars"); // :contentReference[oaicite:21]{index=21}
      stars = asDouble(s);
    }

    return new PlaceDetails(openingHours, stars);
  }

  // Best-effort para open_now usando patrones comunes (24/7, Mo-Su HH:MM-HH:MM, etc.)
  private Boolean computeOpenNowBestEffort(String oh) {
    if (oh == null || oh.isBlank()) return null;
    String v = oh.trim();

    if (v.equalsIgnoreCase("24/7")) return true;

    // Muy básico: "Mo-Su 08:00-22:00" o "Mo-Fr 09:00-18:00" (y variantes con varios segmentos por ;)
    LocalDateTime now = LocalDateTime.now();
    DayOfWeek dow = now.getDayOfWeek();
    LocalTime t = now.toLocalTime();

    for (String segment : v.split(";")) {
      String s = segment.trim();
      if (s.isEmpty()) continue;
      if (s.toLowerCase().contains("off") || s.toLowerCase().contains("closed")) continue;

      ParsedRule rule = parseSimpleRule(s);
      if (rule == null) continue;

      if (rule.days.contains(dow)) {
        if (!t.isBefore(rule.start) && t.isBefore(rule.end)) return true;
      }
    }
    return null; // no reconocido => unknown
  }

  private static final Pattern SIMPLE =
    Pattern.compile("^(Mo|Tu|We|Th|Fr|Sa|Su)(?:-(Mo|Tu|We|Th|Fr|Sa|Su))?\\s+(\\d{1,2}):(\\d{2})-(\\d{1,2}):(\\d{2})$", Pattern.CASE_INSENSITIVE);

  private ParsedRule parseSimpleRule(String s) {
    Matcher m = SIMPLE.matcher(s);
    if (!m.find()) return null;

    DayOfWeek d1 = mapDay(m.group(1));
    DayOfWeek d2 = m.group(2) != null ? mapDay(m.group(2)) : d1;
    if (d1 == null || d2 == null) return null;

    LocalTime start = LocalTime.of(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));
    LocalTime end = LocalTime.of(Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6)));

    Set<DayOfWeek> days = expandDays(d1, d2);
    return new ParsedRule(days, start, end);
  }

  private DayOfWeek mapDay(String abbr) {
    return switch (abbr.toLowerCase()) {
      case "mo" -> DayOfWeek.MONDAY;
      case "tu" -> DayOfWeek.TUESDAY;
      case "we" -> DayOfWeek.WEDNESDAY;
      case "th" -> DayOfWeek.THURSDAY;
      case "fr" -> DayOfWeek.FRIDAY;
      case "sa" -> DayOfWeek.SATURDAY;
      case "su" -> DayOfWeek.SUNDAY;
      default -> null;
    };
  }

  private Set<DayOfWeek> expandDays(DayOfWeek start, DayOfWeek end) {
    Set<DayOfWeek> out = new LinkedHashSet<>();
    DayOfWeek cur = start;
    out.add(cur);
    while (cur != end) {
      cur = cur.plus(1);
      out.add(cur);
    }
    return out;
  }

  private String asString(Object o) {
    return o == null ? null : String.valueOf(o);
  }

  private Double asDouble(Object o) {
    if (o == null) return null;
    if (o instanceof Number n) return n.doubleValue();
    try { return Double.parseDouble(String.valueOf(o)); }
    catch (Exception e) { return null; }
  }

  private record PlaceDetails(String openingHours, Double stars) {}
  private record ParsedRule(Set<DayOfWeek> days, LocalTime start, LocalTime end) {}
}
