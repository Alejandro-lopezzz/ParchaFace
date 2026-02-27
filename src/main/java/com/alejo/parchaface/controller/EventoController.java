package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.CrearEventoDTO;
import com.alejo.parchaface.dto.CrearEventoForm;
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

  // =========================
  // GET TODOS
  // =========================
  @GetMapping
  public List<Evento> obtenerTodos() {
    return eventoService.getAllEventos();
  }

  // =========================
  // GET POR ID
  // =========================
  @GetMapping("/{id}")
  public Evento obtenerPorId(@PathVariable Integer id) {
    return eventoService.getEventoById(id);
  }

  // =========================
  // POST CREAR — JSON (legacy / DTO viejo)
  // =========================
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

  // =========================
  // POST CREAR — MULTIPART/FORM-DATA (nuevo, con archivo real)
  // =========================
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
  public ResponseEntity<Evento> crearEventoForm(
    @Valid @ModelAttribute CrearEventoForm form,
    Authentication authentication
  ) {
    Usuario organizador = getOrganizador(authentication);
    var imagen = form.getImagenPortada();
    Evento evento = eventoService.crearEvento(form, imagen, organizador);
    return ResponseEntity.status(HttpStatus.CREATED).body(evento);
  }

  // =========================
  // POST GUARDAR BORRADOR — MULTIPART/FORM-DATA
  // =========================
  @PostMapping(path = "/borrador", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
  public ResponseEntity<Evento> guardarBorrador(
    @Valid @ModelAttribute CrearEventoForm form,
    Authentication authentication
  ) {
    Usuario organizador = getOrganizador(authentication);
    var imagen = form.getImagenPortada();
    Evento evento = eventoService.crearEvento(form, imagen, organizador, EstadoEvento.borrador);
    return ResponseEntity.status(HttpStatus.CREATED).body(evento);
  }

  @GetMapping("/estado/{estado}")
  public List<Evento> obtenerPorEstado(@PathVariable EstadoEvento estado) {
    return eventoService.getEventosPorEstado(estado);
  }

  // =========================
  // PUT
  // =========================
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('USUARIO') or hasAuthority('ADMINISTRADOR')")
  public Evento actualizar(@PathVariable Integer id, @RequestBody Evento cambios, Authentication authentication) {

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

    existente.setTitulo(cambios.getTitulo());
    existente.setDescripcion(cambios.getDescripcion());
    existente.setCategoria(cambios.getCategoria());

    existente.setFecha(cambios.getFecha());
    existente.setHoraInicio(cambios.getHoraInicio());
    existente.setHoraFin(cambios.getHoraFin());

    existente.setEventoEnLinea(cambios.getEventoEnLinea());
    existente.setUrlVirtual(cambios.getUrlVirtual());
    existente.setUbicacion(cambios.getUbicacion());
    existente.setNombreLugar(cambios.getNombreLugar());
    existente.setDireccionCompleta(cambios.getDireccionCompleta());
    existente.setCiudad(cambios.getCiudad());

    existente.setCupo(cambios.getCupo());
    existente.setEventoGratuito(cambios.getEventoGratuito());
    existente.setPrecio(cambios.getPrecio());

    existente.setEmailContacto(cambios.getEmailContacto());
    existente.setTelefonoContacto(cambios.getTelefonoContacto());
    existente.setSitioWeb(cambios.getSitioWeb());

    existente.setEventoPublico(cambios.getEventoPublico());
    existente.setDetallePrivado(cambios.getDetallePrivado());

    existente.setPermitirComentarios(cambios.getPermitirComentarios());
    existente.setRecordatoriosAutomaticos(cambios.getRecordatoriosAutomaticos());

    return eventoService.saveEvento(existente);
  }

  // =========================
  // DELETE
  // =========================
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

    eventoService.deleteEvento(id);
    return ResponseEntity.noContent().build();
  }

  // =========================
  // Helper auth
  // =========================
  private Usuario getOrganizador(Authentication authentication) {
    if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
    }
    String correo = authentication.getName();
    return usuarioService.getUsuarioPorCorreo(correo);
  }
}
