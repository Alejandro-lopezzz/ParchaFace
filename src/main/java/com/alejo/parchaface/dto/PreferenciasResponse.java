package com.alejo.parchaface.dto;

import java.util.List;

public record PreferenciasResponse(
    boolean completed,
    List<String> categories
) {}
