package com.alejo.parchaface.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String usuario,
        @NotBlank String contrasena,
        @NotBlank String confirmarContrasena,  // Nuevo campo para confirmar contraseña
        @Email @NotBlank String correo
) {}
