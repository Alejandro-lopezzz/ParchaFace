package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.Estado;
import com.alejo.parchaface.repository.UsuarioRepository;
import com.alejo.parchaface.service.UsuarioSuspensionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class UsuarioSuspensionServiceImpl implements UsuarioSuspensionService {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private final UsuarioRepository usuarioRepository;

  public UsuarioSuspensionServiceImpl(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  @Override
  @Transactional
  public Usuario refrescarEstadoSiSuspensionExpirada(Usuario usuario) {
    if (usuario == null || usuario.getEstado() != Estado.SUSPENDIDO) {
      return usuario;
    }

    LocalDateTime suspensionHasta = usuario.getSuspensionHasta();
    if (suspensionHasta == null) {
      return usuario;
    }

    if (LocalDateTime.now().isBefore(suspensionHasta)) {
      return usuario;
    }

    usuario.setEstado(Estado.ACTIVO);
    usuario.setSuspensionHasta(null);
    return usuarioRepository.save(usuario);
  }

  @Override
  public void validarNoSuspendidoParaEventosEInscripciones(Usuario usuario) {
    Usuario actualizado = refrescarEstadoSiSuspensionExpirada(usuario);

    if (actualizado == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado");
    }

    if (actualizado.getEstado() != Estado.SUSPENDIDO) {
      return;
    }

    String detalle = actualizado.getSuspensionHasta() != null
      ? " hasta el " + actualizado.getSuspensionHasta().format(FORMATTER)
      : " de forma indefinida";

    throw new ResponseStatusException(
      HttpStatus.FORBIDDEN,
      "No puedes realizar esta acción porque te encuentras suspendido" + detalle
    );
  }

  @Override
  public LocalDateTime calcularFechaFin(String duracion) {
    String valor = normalizarDuracion(duracion);
    LocalDateTime ahora = LocalDateTime.now();

    return switch (valor) {
      case "1_SEMANA" -> ahora.plusWeeks(1);
      case "2_SEMANAS" -> ahora.plusWeeks(2);
      case "1_MES" -> ahora.plusMonths(1);
      case "3_MESES" -> ahora.plusMonths(3);
      case "INDEFINIDA", "INDEFINIDO", "" -> null;
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duración de suspensión inválida");
    };
  }

  @Override
  public String describirDuracion(String duracion, LocalDateTime suspensionHasta) {
    String valor = normalizarDuracion(duracion);

    return switch (valor) {
      case "1_SEMANA" -> "por 1 semana";
      case "2_SEMANAS" -> "por 2 semanas";
      case "1_MES" -> "por 1 mes";
      case "3_MESES" -> "por 3 meses";
      default -> suspensionHasta != null
        ? "hasta el " + suspensionHasta.format(FORMATTER)
        : "de forma indefinida";
    };
  }

  private String normalizarDuracion(String duracion) {
    if (duracion == null) {
      return "";
    }

    return duracion.trim().toUpperCase(Locale.ROOT);
  }
}
