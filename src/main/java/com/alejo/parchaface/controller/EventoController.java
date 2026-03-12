package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.CrearEventoDTO;
import com.alejo.parchaface.dto.CrearEventoForm;
import com.alejo.parchaface.dto.EventoDetalleResponse;
import com.alejo.parchaface.dto.EventoMapaDTO;
import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.EstadoEvento;
import com.alejo.parchaface.service.EventoService;
import com.alejo.parchaface.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/eventos")
public class EventoController {

  private final EventoService eventoService;
  private final UsuarioService usuarioService;

  public EventoController(EventoService eventoService, UsuarioService usuarioService) {
    this.eventoService = eventoService;
    this.usuarioService = usuarioService;
  }

  @GetMapping
  public List<Evento> obtenerTodos() {
    return eventoService.getAllEventos();
  }

  @GetMapping("/{id}")
  public EventoDetalleResponse obtenerPorId(@PathVariable Integer id) {
    return eventoService.getDetalleEventoById(id);
  }

  @GetMapping("/public")
  public List<EventoMapaDTO> obtenerEventosPublicosParaMapa() {
    return eventoService.getAllEventos()
      .stream()
      .filter(evento -> Boolean.TRUE.equals(evento.getEventoPublico()))
      .filter(evento -> Boolean.FALSE.equals(evento.getEventoEnLinea()))
      .filter(evento -> evento.getLatitud() != null && evento.getLongitud() != null)
      .filter(evento -> evento.getEstadoEvento() != null
        && "activo".equalsIgnoreCase(String.valueOf(evento.getEstadoEvento())))
      .map(evento -> new EventoMapaDTO(
        evento.getIdEvento(),
        evento.getTitulo(),
        evento.getCategoria(),
        evento.getFecha(),
        evento.getCiudad(),
        evento.getNombreLugar(),
        evento.getImagenPortadaUrl(),
        evento.getLatitud(),
        evento.getLongitud()
      ))
      .toList();
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
  public ResponseEntity<Evento> crearEventoJson(
    @Valid @RequestBody CrearEventoDTO dto,
    Authentication authentication
  ) {
    Usuario organizador = getOrganizador(authentication);
    Evento evento = eventoService.crearEvento(dto, organizador);
    return ResponseEntity.status(HttpStatus.CREATED).body(evento);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
  public ResponseEntity<Evento> crearEventoForm(
    @Valid @ModelAttribute CrearEventoForm form,
    Authentication authentication
  ) {
    Usuario organizador = getOrganizador(authentication);
    Evento evento = eventoService.crearEvento(form, form.getImagenPortada(), organizador);
    return ResponseEntity.status(HttpStatus.CREATED).body(evento);
  }

  @PostMapping(path = "/borrador", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
  public ResponseEntity<Evento> guardarBorrador(
    @Valid @ModelAttribute CrearEventoForm form,
    Authentication authentication
  ) {
    Usuario organizador = getOrganizador(authentication);
    Evento evento = eventoService.crearEvento(
      form,
      form.getImagenPortada(),
      organizador,
      EstadoEvento.borrador
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(evento);
  }

  @GetMapping("/estado/{estado}")
  public List<Evento> obtenerPorEstado(@PathVariable EstadoEvento estado) {
    return eventoService.getEventosPorEstado(estado);
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

    boolean esAdmin = authentication.getAuthorities().stream()
      .anyMatch(a -> a.getAuthority().equals("ADMINISTRADOR"));

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

    boolean esAdmin = authentication.getAuthorities().stream()
      .anyMatch(a -> a.getAuthority().equals("ADMINISTRADOR"));

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
    Usuario usuario = usuarioService.getUsuarioPorCorreo(correo);

    if (usuario == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado");
    }

    return usuario;
  }
}
