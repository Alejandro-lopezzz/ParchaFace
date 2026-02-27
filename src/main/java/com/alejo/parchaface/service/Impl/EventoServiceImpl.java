package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.dto.CrearEventoDTO;
import com.alejo.parchaface.dto.CrearEventoForm;
import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.EstadoEvento;
import com.alejo.parchaface.repository.EventoRepository;
import com.alejo.parchaface.service.EventoService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EventoServiceImpl implements EventoService {

  private final EventoRepository eventoRepository;

  // ✅ Valor fijo para cumplir NOT NULL cuando el evento es en línea
  private static final String LUGAR_EN_LINEA = "EN LINEA";

  // Carpeta física donde se guardan las portadas (relativa al root del proyecto)
  private static final Path UPLOAD_DIR = Paths.get("uploads", "eventos");

  // URL pública (depende del WebConfig ResourceHandler)
  private static final String PUBLIC_URL_PREFIX = "/uploads/eventos/";

  public EventoServiceImpl(EventoRepository eventoRepository) {
    this.eventoRepository = eventoRepository;
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
  public List<Evento> getEventosPublicos() {
    // Si no tienes filtros, devuelve todos por ahora:
    return eventoRepository.findAll();

    // Si tienes estado/publico, aquí se filtra:
    // return eventoRepository.findByEventoPublicoTrue();
  }

  @Override
  public Evento saveEvento(Evento evento) {
    return eventoRepository.save(evento);
  }

  @Override
  public void deleteEvento(Integer id) {
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

    // Imagen (en este flujo sigue siendo URL + contentType)
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

      // ✅ FIX: NO dejar lugar/ubicación en null (DB: Column 'lugar' NOT NULL)
      evento.setUbicacion(LUGAR_EN_LINEA);

      evento.setNombreLugar(null);
      evento.setDireccionCompleta(null);
      evento.setCiudad(null);
    } else {
      evento.setUrlVirtual(null);

      // Presencial
      evento.setUbicacion(dto.getUbicacion());
      evento.setNombreLugar(dto.getNombreLugar());
      evento.setDireccionCompleta(dto.getDireccionCompleta());
      evento.setCiudad(dto.getCiudad());
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

      // ✅ FIX: NO dejar lugar/ubicación en null (DB: Column 'lugar' NOT NULL)
      evento.setUbicacion(LUGAR_EN_LINEA);

      evento.setNombreLugar(null);
      evento.setDireccionCompleta(null);
      evento.setCiudad(null);
    } else {
      evento.setUrlVirtual(null);

      // Presencial
      evento.setUbicacion(form.getUbicacion());
      evento.setNombreLugar(form.getNombreLugar());
      evento.setDireccionCompleta(form.getDireccionCompleta());
      evento.setCiudad(form.getCiudad());
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

    // ======================
    // Imagen (archivo real)
    // ======================
    MultipartFile file = imagenPortada;
    if (file == null) {
      try {
        file = form.getImagenPortada();
      } catch (Exception ignored) {}
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
  // CREAR EVENTO — FORM-DATA con estado específico (borradores)
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

      // ✅ FIX: NO dejar lugar/ubicación en null (DB: Column 'lugar' NOT NULL)
      evento.setUbicacion(LUGAR_EN_LINEA);

      evento.setNombreLugar(null);
      evento.setDireccionCompleta(null);
      evento.setCiudad(null);
    } else {
      evento.setUrlVirtual(null);

      // Presencial
      evento.setUbicacion(form.getUbicacion());
      evento.setNombreLugar(form.getNombreLugar());
      evento.setDireccionCompleta(form.getDireccionCompleta());
      evento.setCiudad(form.getCiudad());
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

    // ======================
    // Imagen (archivo real)
    // ======================
    MultipartFile file = imagenPortada;
    if (file == null) {
      try {
        file = form.getImagenPortada();
      } catch (Exception ignored) {}
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

      String contentType = (file.getContentType() == null) ? "" : file.getContentType().toLowerCase().trim();

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

  private record SavedImage(String publicUrl, String contentType) {}
}
