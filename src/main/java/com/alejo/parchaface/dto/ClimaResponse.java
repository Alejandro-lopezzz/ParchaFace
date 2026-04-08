package com.alejo.parchaface.dto;

import java.util.List;

public record ClimaResponse(
        String ciudad,
        String pais,
        double lat,
        double lon,
        double temperaturaC,
        double vientoKmh,
        int codigoClima,
        String timezone,
        List<ClimaPronosticoDia> pronosticoDias
) {
}