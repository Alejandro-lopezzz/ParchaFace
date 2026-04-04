package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.CrearEventoDTO;
import com.alejo.parchaface.dto.CrearEventoForm;
import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.EstadoEvento;
import com.alejo.parchaface.service.EventoService;
import com.alejo.parchaface.service.UsuarioService;
import com.alejo.parchaface.service.UsuarioSuspensionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/eventos")
public class EventoController {

  private final EventoService eventoService;
  private final UsuarioService usuarioService;
  private final UsuarioSuspensionService usuarioSuspensionService;

  public EventoController(
    EventoService eventoService,
    UsuarioService usuarioService,
    UsuarioSuspensionService usuarioSuspensionService
  ) {
    this.eventoService = eventoService;
    this.usuarioService = usuarioService;
    this.usuarioSuspensionService = usuarioSuspensionService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('ADMINISTRADOR')")
  public List<Evento> obtenerTodos() {
    return eventoService.getAllEventos();
  }

  @GetMapping("/{id}")
  public Evento obtenerPorId(@PathVariable Integer id, Authentication authentication) {
    Evento evento = eventoService.getEventoById(id);
    if (evento == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado");
    }

    if (evento.getEstadoEvento() == EstadoEvento.activo) {
      return evento;
    }

    if (authentication == null || authentication.getName() == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado para ver este evento");
    }

    boolean esAdmin = esAdmin(authentication);
    boolean esOrganizador = evento.getOrganizador() != null
      && evento.getOrganizador().getCorreo() != null
      && evento.getOrganizador().getCorreo().equalsIgnoreCase(authentication.getName());

    if (!esAdmin && !esOrganizador) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado para ver este evento");
    }

    return evento;
  }

  @GetMapping("/public")
  public List<Evento> obtenerEventosPublicos() {
    return eventoService.getEventosPublicos();
  }

  @GetMapping("/estado/{estado}")
  @PreAuthorize("hasAuthority('ADMINISTRADOR')")
  public List<Evento> obtenerPorEstado(@PathVariable EstadoEvento estado) {
    return eventoService.getEventosPorEstado(estado);
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
  public ResponseEntity<?> crearEventoJson(
    @Valid @RequestBody CrearEventoDTO dto,
    Authentication authentication
  ) {
    Usuario organizador = getOrganizador(authentication);

    if (!esAdmin(authentication)) {
      usuarioSuspensionService.validarNoSuspendidoParaEventosEInscripciones(organizador);
    }

    Evento evento = eventoService.crearEvento(dto, organizador);
    return ResponseEntity.status(HttpStatus.CREATED).body(evento);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
  public ResponseEntity<?> crearEventoForm(
    @Valid @ModelAttribute CrearEventoForm form,
    Authentication authentication
  ) {
    Usuario organizador = getOrganizador(authentication);

    if (esAdmin(authentication)) {
      Evento evento = eventoService.crearEvento(form, organizador, EstadoEvento.activo);
      return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
        "mensaje", "Evento creado y publicado por administrador",
        "evento", evento
      ));
    }

    usuarioSuspensionService.validarNoSuspendidoParaEventosEInscripciones(organizador);

    Evento evento = eventoService.solicitarCreacionEvento(form, organizador);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
      "mensaje", "La solicitud de creación de evento fue enviada.",
      "evento", evento,
      "estado", evento.getEstadoEvento().name()
    ));
  }

  @PostMapping(path = "/borrador", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
  public ResponseEntity<Evento> guardarBorrador(
    @Valid @ModelAttribute CrearEventoForm form,
    Authentication authentication
  ) {
    Usuario organizador = getOrganizador(authentication);

    if (!esAdmin(authentication)) {
      usuarioSuspensionService.validarNoSuspendidoParaEventosEInscripciones(organizador);
    }

    Evento evento = eventoService.crearEvento(form, organizador, EstadoEvento.borrador);
    return ResponseEntity.status(HttpStatus.CREATED).body(evento);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
  public Evento actualizar(
    @PathVariable Integer id,
    @RequestBody Evento cambios,
    Authentication authentication
  ) {
    Evento existente = eventoService.getEventoById(id);
    if (existente == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado");
    }

    boolean esAdmin = esAdmin(authentication);
    String correoActual = authentication.getName();

    if (!esAdmin) {
      if (existente.getOrganizador() == null || existente.getOrganizador().getCorreo() == null) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
      }
      if (!existente.getOrganizador().getCorreo().equalsIgnoreCase(correoActual)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el creador puede editar este evento");
      }
    }

    return eventoService.actualizarEventoYNotificar(id, cambios);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
  public ResponseEntity<Void> eliminar(@PathVariable Integer id, Authentication authentication) {
    Evento existente = eventoService.getEventoById(id);
    if (existente == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado");
    }

    boolean esAdmin = esAdmin(authentication);
    String correoActual = authentication.getName();

    if (!esAdmin) {
      if (existente.getOrganizador() == null || existente.getOrganizador().getCorreo() == null) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
      }
      if (!existente.getOrganizador().getCorreo().equalsIgnoreCase(correoActual)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el creador puede eliminar este evento");
      }
    }

    eventoService.eliminarEventoYNotificar(id);
    return ResponseEntity.noContent().build();
  }

  private Usuario getOrganizador(Authentication authentication) {
    if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
    }

    String correo = authentication.getName();
    return usuarioService.getUsuarioPorCorreo(correo);
  }

  private boolean esAdmin(Authentication authentication) {
    return authentication.getAuthorities().stream()
      .anyMatch(a -> a.getAuthority().equals("ADMINISTRADOR"));
  }
}
