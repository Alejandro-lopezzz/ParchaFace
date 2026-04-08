package com.alejo.parchaface.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEventoCommentRequest(
        @NotBlank @Size(max = 1000) String contenido
) {}