package com.alejo.parchaface.service;

import com.alejo.parchaface.model.Usuario;

import java.time.LocalDateTime;

public interface UsuarioSuspensionService {

  Usuario refrescarEstadoSiSuspensionExpirada(Usuario usuario);

  void validarNoSuspendidoParaEventosEInscripciones(Usuario usuario);

  LocalDateTime calcularFechaFin(String duracion);

  String describirDuracion(String duracion, LocalDateTime suspensionHasta);
}
