package com.alejo.parchaface.dto;

import com.alejo.parchaface.model.enums.PlataformaSocial;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RedSocialEventoForm {

    @NotNull(message = "La plataforma es obligatoria")
    private PlataformaSocial plataforma;

    @NotBlank(message = "La URL de la red social es obligatoria")
    @Size(max = 500, message = "La URL no puede superar los 500 caracteres")
    @Pattern(regexp = "^(https?://).+$", message = "La URL debe iniciar con http:// o https://")
    private String url;

    public PlataformaSocial getPlataforma() {
        return plataforma;
    }

    public void setPlataforma(PlataformaSocial plataforma) {
        this.plataforma = plataforma;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}