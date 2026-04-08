package com.alejo.parchaface.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PreferenciasRequest(
  @NotNull(message = "categories no puede ser null")
  @JsonAlias({"categorias", "categoriasPreferidas"})
  List<String> categories
) {}
