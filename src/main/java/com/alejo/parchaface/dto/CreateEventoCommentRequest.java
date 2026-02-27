package com.alejo.parchaface.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEventoCommentRequest(
        @NotBlank @Size(max = 1000) String contenido
) {}