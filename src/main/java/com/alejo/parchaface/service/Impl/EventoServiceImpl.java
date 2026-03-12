package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.dto.CrearEventoDTO;
import com.alejo.parchaface.dto.CrearEventoForm;
import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.Inscripcion;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.EstadoEvento;
import com.alejo.parchaface.repository.EventoCommentRepository;
import com.alejo.parchaface.repository.EventoRepository;
import com.alejo.parchaface.repository.InscripcionRepository;
import com.alejo.parchaface.service.EventoService;
import com.alejo.parchaface.service.NotificacionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.alejo.parchaface.dto.EventoDetalleResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EventoServiceImpl implements EventoService {

  private final EventoRepository eventoRepository;
  private final InscripcionRepository inscripcionRepository;
  private final NotificacionService notificacionService;
  private final EventoCommentRepository eventoCommentRepository;

  // Valor fijo para cumplir NOT NULL cuando el evento es en línea
  private static final String LUGAR_EN_LINEA = "EN LINEA";

  // Carpeta física donde se guardan las portadas
  private static final Path UPLOAD_DIR = Paths.get("uploads", "eventos");

  // URL pública
  private static final String PUBLIC_URL_PREFIX = "/uploads/eventos/";

  public EventoServiceImpl(
          EventoRepository eventoRepository,
          InscripcionRepository inscripcionRepository,
          NotificacionService notificacionService,
          EventoCommentRepository eventoCommentRepository
  ) {
    this.eventoRepository = eventoRepository;
    this.inscripcionRepository = inscripcionRepository;
    this.notificacionService = notificacionService;
    this.eventoCommentRepository = eventoCommentRepository;
  }

  @Override
  public List<Evento> getAllEventos() {
    return eventoRepository.findAll();
  }

  @Override
  public Evento getEventoById(Integer id) {
    Optional<Evento> evento = eventoRepository.findById(id);
    return evento.orElse(null);
  }

  @Override
  public Evento saveEvento(Evento evento) {
    return eventoRepository.save(evento);
  }

  @Override
  @Transactional
  public void deleteEvento(Integer id) {
    eventoCommentRepository.deleteByEvento_IdEvento(id);
    eventoRepository.deleteById(id);
  }

  @Override
  public List<Evento> getEventosPorEstado(EstadoEvento estadoEvento) {
    return eventoRepository.findByEstadoEvento(estadoEvento);
  }

  // =========================
  // CREAR EVENTO — JSON (DTO viejo)
  // =========================
  @Override
  public Evento crearEvento(CrearEventoDTO dto, Usuario organizador) {
    Evento evento = new Evento();

    // Básicos
    evento.setTitulo(dto.getTitulo());
    evento.setDescripcion(dto.getDescripcion());
    evento.setCategoria(dto.getCategoria());

    // Imagen (flujo DTO viejo)
    evento.setImagenPortadaUrl(dto.getImagenPortadaUrl());
    evento.setImagenPortadaContentType(dto.getImagenPortadaContentType());

    // Fecha/hora
    evento.setFecha(LocalDateTime.of(dto.getFecha(), dto.getHoraInicio()));
    evento.setHoraInicio(dto.getHoraInicio());
    evento.setHoraFin(dto.getHoraFin());

    // Modalidad
    boolean enLinea = Boolean.TRUE.equals(dto.getEventoEnLinea());
    evento.setEventoEnLinea(enLinea);

    if (enLinea) {
      evento.setUrlVirtual(dto.getUrlVirtual());
      evento.setUbicacion(LUGAR_EN_LINEA);
      evento.setNombreLugar(null);
      evento.setDireccionCompleta(null);
      evento.setCiudad(null);

      // Coordenadas no aplican para evento en línea
      evento.setLatitud(null);
      evento.setLongitud(null);
    } else {
      evento.setUrlVirtual(null);
      evento.setUbicacion(dto.getUbicacion());
      evento.setNombreLugar(dto.getNombreLugar());
      evento.setDireccionCompleta(dto.getDireccionCompleta());
      evento.setCiudad(dto.getCiudad());

      // OJO:
      // Esto solo compila si también agregas latitud/longitud al CrearEventoDTO
      evento.setLatitud(dto.getLatitud());
      evento.setLongitud(dto.getLongitud());
    }

    // Cupo / Precio
    evento.setCupo(dto.getCupo());

    boolean gratuito = Boolean.TRUE.equals(dto.getEventoGratuito());
    evento.setEventoGratuito(gratuito);
    evento.setPrecio(gratuito ? null : dto.getPrecio());

    // Contacto
    evento.setEmailContacto(dto.getEmailContacto());
    evento.setTelefonoContacto(dto.getTelefonoContacto());
    evento.setSitioWeb(dto.getSitioWeb());

    // Privacidad
    boolean publico = Boolean.TRUE.equals(dto.getEventoPublico());
    evento.setEventoPublico(publico);
    evento.setDetallePrivado(publico ? null : dto.getDetallePrivado());

    // Config
    evento.setPermitirComentarios(Boolean.TRUE.equals(dto.getPermitirComentarios()));
    evento.setRecordatoriosAutomaticos(Boolean.TRUE.equals(dto.getRecordatoriosAutomaticos()));

    // Defaults / relaciones
    evento.setEstadoEvento(EstadoEvento.activo);
    evento.setOrganizador(organizador);

    return eventoRepository.save(evento);
  }

  // =========================
  // CREAR EVENTO — FORM-DATA (nuevo)
  // =========================
  @Override
  public Evento crearEvento(CrearEventoForm form, MultipartFile imagenPortada, Usuario organizador) {
    Evento evento = new Evento();

    // Básicos
    evento.setTitulo(form.getTitulo());
    evento.setDescripcion(form.getDescripcion());
    evento.setCategoria(form.getCategoria());

    // Fecha/hora
    evento.setFecha(LocalDateTime.of(form.getFecha(), form.getHoraInicio()));
    evento.setHoraInicio(form.getHoraInicio());
    evento.setHoraFin(form.getHoraFin());

    // Modalidad
    boolean enLinea = Boolean.TRUE.equals(form.getEventoEnLinea());
    evento.setEventoEnLinea(enLinea);

    if (enLinea) {
      evento.setUrlVirtual(form.getUrlVirtual());
      evento.setUbicacion(LUGAR_EN_LINEA);
      evento.setNombreLugar(null);
      evento.setDireccionCompleta(null);
      evento.setCiudad(null);
      evento.setLatitud(null);
      evento.setLongitud(null);
    } else {
      evento.setUrlVirtual(null);
      evento.setUbicacion(form.getUbicacion());
      evento.setNombreLugar(form.getNombreLugar());
      evento.setDireccionCompleta(form.getDireccionCompleta());
      evento.setCiudad(form.getCiudad());
      evento.setLatitud(form.getLatitud());
      evento.setLongitud(form.getLongitud());
    }

    // Cupo / Precio
    evento.setCupo(form.getCupo());

    boolean gratuito = Boolean.TRUE.equals(form.getEventoGratuito());
    evento.setEventoGratuito(gratuito);
    evento.setPrecio(gratuito ? null : form.getPrecio());

    // Contacto
    evento.setEmailContacto(form.getEmailContacto());
    evento.setTelefonoContacto(form.getTelefonoContacto());
    evento.setSitioWeb(form.getSitioWeb());

    // Privacidad
    boolean publico = Boolean.TRUE.equals(form.getEventoPublico());
    evento.setEventoPublico(publico);
    evento.setDetallePrivado(publico ? null : form.getDetallePrivado());

    // Config
    evento.setPermitirComentarios(Boolean.TRUE.equals(form.getPermitirComentarios()));
    evento.setRecordatoriosAutomaticos(Boolean.TRUE.equals(form.getRecordatoriosAutomaticos()));

    // Imagen
    MultipartFile file = imagenPortada;
    if (file == null) {
      try {
        file = form.getImagenPortada();
      } catch (Exception ignored) {
      }
    }

    if (file != null && !file.isEmpty()) {
      SavedImage saved = saveCoverToDisk(file);
      evento.setImagenPortadaUrl(saved.publicUrl());
      evento.setImagenPortadaContentType(saved.contentType());
    } else {
      evento.setImagenPortadaUrl(null);
      evento.setImagenPortadaContentType(null);
    }

    // Defaults / relaciones
    evento.setEstadoEvento(EstadoEvento.activo);
    evento.setOrganizador(organizador);

    return eventoRepository.save(evento);
  }

  // =========================
  // CREAR EVENTO — FORM-DATA con estado específico
  // =========================
  @Override
  public Evento crearEvento(CrearEventoForm form, MultipartFile imagenPortada, Usuario organizador, EstadoEvento estado) {
    Evento evento = new Evento();

    // Básicos
    evento.setTitulo(form.getTitulo());
    evento.setDescripcion(form.getDescripcion());
    evento.setCategoria(form.getCategoria());

    // Fecha/hora
    evento.setFecha(LocalDateTime.of(form.getFecha(), form.getHoraInicio()));
    evento.setHoraInicio(form.getHoraInicio());
    evento.setHoraFin(form.getHoraFin());

    // Modalidad
    boolean enLinea = Boolean.TRUE.equals(form.getEventoEnLinea());
    evento.setEventoEnLinea(enLinea);

    if (enLinea) {
      evento.setUrlVirtual(form.getUrlVirtual());
      evento.setUbicacion(LUGAR_EN_LINEA);
      evento.setNombreLugar(null);
      evento.setDireccionCompleta(null);
      evento.setCiudad(null);
      evento.setLatitud(null);
      evento.setLongitud(null);
    } else {
      evento.setUrlVirtual(null);
      evento.setUbicacion(form.getUbicacion());
      evento.setNombreLugar(form.getNombreLugar());
      evento.setDireccionCompleta(form.getDireccionCompleta());
      evento.setCiudad(form.getCiudad());
      evento.setLatitud(form.getLatitud());
      evento.setLongitud(form.getLongitud());
    }

    // Cupo / Precio
    evento.setCupo(form.getCupo());

    boolean gratuito = Boolean.TRUE.equals(form.getEventoGratuito());
    evento.setEventoGratuito(gratuito);
    evento.setPrecio(gratuito ? null : form.getPrecio());

    // Contacto
    evento.setEmailContacto(form.getEmailContacto());
    evento.setTelefonoContacto(form.getTelefonoContacto());
    evento.setSitioWeb(form.getSitioWeb());

    // Privacidad
    boolean publico = Boolean.TRUE.equals(form.getEventoPublico());
    evento.setEventoPublico(publico);
    evento.setDetallePrivado(publico ? null : form.getDetallePrivado());

    // Config
    evento.setPermitirComentarios(Boolean.TRUE.equals(form.getPermitirComentarios()));
    evento.setRecordatoriosAutomaticos(Boolean.TRUE.equals(form.getRecordatoriosAutomaticos()));

    // Imagen
    MultipartFile file = imagenPortada;
    if (file == null) {
      try {
        file = form.getImagenPortada();
      } catch (Exception ignored) {
      }
    }

    if (file != null && !file.isEmpty()) {
      SavedImage saved = saveCoverToDisk(file);
      evento.setImagenPortadaUrl(saved.publicUrl());
      evento.setImagenPortadaContentType(saved.contentType());
    } else {
      evento.setImagenPortadaUrl(null);
      evento.setImagenPortadaContentType(null);
    }

    // Relaciones y estado
    evento.setOrganizador(organizador);
    evento.setEstadoEvento(estado != null ? estado : EstadoEvento.activo);

    return eventoRepository.save(evento);
  }

  // =========================
  // Guardado de imagen
  // =========================
  private SavedImage saveCoverToDisk(MultipartFile file) {
    try {
      Files.createDirectories(UPLOAD_DIR);

      String contentType = (file.getContentType() == null)
              ? ""
              : file.getContentType().toLowerCase().trim();

      boolean ok = contentType.equals("image/jpeg")
              || contentType.equals("image/jpg")
              || contentType.equals("image/png")
              || contentType.equals("image/webp");

      if (!ok) {
        throw new IllegalArgumentException("Tipo de imagen no permitido: " + contentType);
      }

      String originalName = StringUtils.cleanPath(
              file.getOriginalFilename() == null ? "" : file.getOriginalFilename()
      );

      String ext = getExtension(originalName);

      if (ext.isBlank()) {
        ext = switch (contentType) {
          case "image/png" -> "png";
          case "image/webp" -> "webp";
          default -> "jpg";
        };
      }

      String filename = UUID.randomUUID() + "." + ext;

      Path target = UPLOAD_DIR.resolve(filename).normalize();

      if (!target.startsWith(UPLOAD_DIR.normalize())) {
        throw new IllegalArgumentException("Ruta inválida para guardar archivo");
      }

      Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

      String publicUrl = PUBLIC_URL_PREFIX + filename;
      return new SavedImage(publicUrl, contentType);

    } catch (IOException e) {
      throw new RuntimeException("No se pudo guardar la imagen de portada", e);
    }
  }

  private String getExtension(String filename) {
    if (filename == null) return "";
    int dot = filename.lastIndexOf('.');
    if (dot < 0 || dot == filename.length() - 1) return "";
    return filename.substring(dot + 1).trim().toLowerCase();
  }

  private record SavedImage(String publicUrl, String contentType) {
  }

  @Override
  @Transactional
  public Evento actualizarEventoYNotificar(Integer idEvento, Evento cambios) {
    Evento existente = eventoRepository.findById(idEvento)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));

    boolean cambioClave =
            (cambios.getTitulo() != null && !cambios.getTitulo().equals(existente.getTitulo()))
                    || (cambios.getFecha() != null && !cambios.getFecha().equals(existente.getFecha()))
                    || (cambios.getUbicacion() != null && !cambios.getUbicacion().equals(existente.getUbicacion()));

    if (cambios.getTitulo() != null) existente.setTitulo(cambios.getTitulo());
    if (cambios.getDescripcion() != null) existente.setDescripcion(cambios.getDescripcion());
    if (cambios.getCategoria() != null) existente.setCategoria(cambios.getCategoria());

    if (cambios.getFecha() != null) existente.setFecha(cambios.getFecha());
    if (cambios.getHoraInicio() != null) existente.setHoraInicio(cambios.getHoraInicio());
    if (cambios.getHoraFin() != null) existente.setHoraFin(cambios.getHoraFin());

    if (cambios.getEventoEnLinea() != null) existente.setEventoEnLinea(cambios.getEventoEnLinea());
    if (cambios.getUrlVirtual() != null) existente.setUrlVirtual(cambios.getUrlVirtual());
    if (cambios.getUbicacion() != null) existente.setUbicacion(cambios.getUbicacion());
    if (cambios.getNombreLugar() != null) existente.setNombreLugar(cambios.getNombreLugar());
    if (cambios.getDireccionCompleta() != null) existente.setDireccionCompleta(cambios.getDireccionCompleta());
    if (cambios.getCiudad() != null) existente.setCiudad(cambios.getCiudad());
    if (cambios.getLatitud() != null) existente.setLatitud(cambios.getLatitud());
    if (cambios.getLongitud() != null) existente.setLongitud(cambios.getLongitud());

    if (cambios.getCupo() != null) existente.setCupo(cambios.getCupo());
    if (cambios.getEventoGratuito() != null) existente.setEventoGratuito(cambios.getEventoGratuito());
    if (cambios.getPrecio() != null) existente.setPrecio(cambios.getPrecio());

    if (cambios.getEmailContacto() != null) existente.setEmailContacto(cambios.getEmailContacto());
    if (cambios.getTelefonoContacto() != null) existente.setTelefonoContacto(cambios.getTelefonoContacto());
    if (cambios.getSitioWeb() != null) existente.setSitioWeb(cambios.getSitioWeb());

    if (cambios.getEventoPublico() != null) existente.setEventoPublico(cambios.getEventoPublico());
    if (cambios.getDetallePrivado() != null) existente.setDetallePrivado(cambios.getDetallePrivado());

    if (cambios.getPermitirComentarios() != null) existente.setPermitirComentarios(cambios.getPermitirComentarios());
    if (cambios.getRecordatoriosAutomaticos() != null) existente.setRecordatoriosAutomaticos(cambios.getRecordatoriosAutomaticos());

    Evento actualizado = eventoRepository.save(existente);

    if (cambioClave) {
      List<Inscripcion> inscripciones = inscripcionRepository.findByEvento_IdEvento(actualizado.getIdEvento());
      for (Inscripcion ins : inscripciones) {
        notificacionService.crearNotificacion(
                ins.getUsuario(),
                "El evento \"" + actualizado.getTitulo() + "\" fue actualizado. Revisa los cambios."
        );
      }
    }

    return actualizado;
  }

  @Override
  @Transactional
  public void eliminarEventoYNotificar(Integer idEvento) {
    Evento evento = eventoRepository.findById(idEvento)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));

    List<Inscripcion> inscripciones = inscripcionRepository.findByEvento_IdEvento(idEvento);

    for (Inscripcion ins : inscripciones) {
      notificacionService.crearNotificacion(
              ins.getUsuario(),
              "El evento \"" + evento.getTitulo() + "\" fue eliminado/cancelado. Tu inscripción quedó anulada."
      );
    }

    eventoCommentRepository.deleteByEvento_IdEvento(idEvento);
    eventoRepository.delete(evento);
  }

  @Override
  @Transactional(readOnly = true)
  public EventoDetalleResponse getDetalleEventoById(Integer id) {
    Evento e = eventoRepository.findWithOrganizadorByIdEvento(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));

    return new EventoDetalleResponse(
      e.getIdEvento(),
      e.getTitulo(),
      e.getDescripcion(),
      e.getCategoria(),
      e.getImagenPortadaUrl(),
      e.getFecha(),
      e.getHoraInicio(),
      e.getHoraFin(),
      e.getEventoEnLinea(),
      e.getUrlVirtual(),
      e.getUbicacion(),
      e.getNombreLugar(),
      e.getDireccionCompleta(),
      e.getCiudad(),
      e.getLatitud(),
      e.getLongitud(),
      e.getCupo(),
      e.getEventoGratuito(),
      e.getPrecio(),
      e.getEmailContacto(),
      e.getTelefonoContacto(),
      e.getSitioWeb(),
      e.getEventoPublico(),
      e.getDetallePrivado(),
      e.getPermitirComentarios(),
      e.getRecordatoriosAutomaticos(),
      e.getEstadoEvento() != null ? e.getEstadoEvento().name() : null,
      e.getOrganizador() != null ? e.getOrganizador().getIdUsuario() : null,
      e.getOrganizador() != null ? e.getOrganizador().getNombre() : null,
      e.getOrganizador() != null ? e.getOrganizador().getCorreo() : null
    );
  }
}
