package com.alejo.parchaface.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String usuario,
        @NotBlank String contrasena,
        @Email @NotBlank String correo,
        @NotBlank String rol
) {}
