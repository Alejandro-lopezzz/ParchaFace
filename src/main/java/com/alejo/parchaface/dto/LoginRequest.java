package com.alejo.parchaface.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String correo,
        @NotBlank String contrasena
) {}
