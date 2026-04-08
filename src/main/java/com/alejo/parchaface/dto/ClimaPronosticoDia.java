package com.alejo.parchaface.dto;

public record ClimaPronosticoDia(
        String fecha,
        double temperaturaMaxC,
        double temperaturaMinC,
        int codigoClima
) {
}