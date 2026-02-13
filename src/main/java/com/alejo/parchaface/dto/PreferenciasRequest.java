package com.alejo.parchaface.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PreferenciasRequest(
    @NotNull(message = "categories no puede ser null")
    List<String> categories
) {}
