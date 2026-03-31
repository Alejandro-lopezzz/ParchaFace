package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.dto.CrearEventoDTO;
import com.alejo.parchaface.dto.CrearEventoForm;
import com.alejo.parchaface.dto.RedSocialEventoForm;
import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.EventoImagen;
import com.alejo.parchaface.model.EventoRedSocial;
import com.alejo.parchaface.model.Inscripcion;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.EstadoEvento;
import com.alejo.parchaface.repository.EventoCommentRepository;
import com.alejo.parchaface.repository.EventoRepository;
import com.alejo.parchaface.repository.InscripcionRepository;
import com.alejo.parchaface.service.EventoService;
import com.alejo.parchaface.service.NotificacionService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.alejo.parchaface.model.enums.EstadoInscripcion;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class EventoServiceImpl implements EventoService {

  private final EventoRepository eventoRepository;
  private final InscripcionRepository inscripcionRepository;
  private final NotificacionService notificacionService;
  private final EventoCommentRepository eventoCommentRepository;
  private final Cloudinary cloudinary;

  private static final String LUGAR_EN_LINEA = "EN LINEA";
  private static final int MAX_IMAGENES = 3;
  private static final int MIN_IMAGENES_PUBLICADO = 1;
  private static final String CLOUDINARY_FOLDER_EVENTOS = "parchaface/eventos";

  public EventoServiceImpl(
          EventoRepository eventoRepository,
          InscripcionRepository inscripcionRepository,
          NotificacionService notificacionService,
          EventoCommentRepository eventoCommentRepository,
          Cloudinary cloudinary
  ) {
    this.eventoRepository = eventoRepository;
    this.inscripcionRepository = inscripcionRepository;
    this.notificacionService = notificacionService;
    this.eventoCommentRepository = eventoCommentRepository;
    this.cloudinary = cloudinary;
  }

  @Override
  public List<Evento> getAllEventos() {
    return eventoRepository.findAll();
  }

  @Override
  public List<Evento> getEventosPublicos() {
    List<Evento> eventos = eventoRepository.findByEstadoEvento(EstadoEvento.activo);

    for (Evento evento : eventos) {
      long total = inscripcionRepository.countByEvento_IdEventoAndEstadoInscripcion(
              evento.getIdEvento(),
              EstadoInscripcion.vigente
      );
      evento.setTotalInscritos(total);
    }

    return eventos;
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
    Evento evento = eventoRepository.findById(id).orElse(null);
    if (evento == null) {
      return;
    }

    eliminarImagenesDeCloudinary(evento);
    eventoCommentRepository.deleteByEvento_IdEvento(id);
    eventoRepository.delete(evento);
  }

  @Override
  public List<Evento> getEventosPorEstado(EstadoEvento estadoEvento) {
    return eventoRepository.findByEstadoEvento(estadoEvento);
  }

  // =========================
  // CREAR EVENTO — JSON (legacy)
  // =========================
  @Override
  @Transactional
  public Evento crearEvento(CrearEventoDTO dto, Usuario organizador) {
    Evento evento = new Evento();

    mapearDtoEnEvento(dto, evento);

    evento.setEstadoEvento(EstadoEvento.activo);
    evento.setOrganizador(organizador);

    return eventoRepository.save(evento);
  }

  // =========================
  // CREAR EVENTO — MULTIPART/FORM-DATA
  // Publicado/activo
  // =========================
  @Override
  @Transactional
  public Evento crearEvento(CrearEventoForm form, Usuario organizador) {
    return crearEvento(form, organizador, EstadoEvento.activo);
  }

  // =========================
  // CREAR EVENTO — MULTIPART/FORM-DATA con estado específico
  // =========================
  @Override
  @Transactional
  public Evento crearEvento(CrearEventoForm form, Usuario organizador, EstadoEvento estado) {
    Evento evento = new Evento();

    mapearFormEnEvento(form, evento);
    evento.setOrganizador(organizador);
    evento.setEstadoEvento(estado != null ? estado : EstadoEvento.activo);

    List<MultipartFile> imagenesValidas = normalizarImagenes(form.getImagenes());
    validarCantidadImagenes(imagenesValidas, evento.getEstadoEvento());

    List<String> publicIdsSubidos = new ArrayList<>();

    try {
      procesarRedesSociales(form, evento);
      procesarImagenes(evento, imagenesValidas, publicIdsSubidos);

      return eventoRepository.save(evento);
    } catch (Exception e) {
      limpiarSubidasFallidas(publicIdsSubidos);
      throw e;
    }
  }

  private void mapearDtoEnEvento(CrearEventoDTO dto, Evento evento) {
    // Básicos
    evento.setTitulo(dto.getTitulo());
    evento.setDescripcion(dto.getDescripcion());
    evento.setCategoria(dto.getCategoria());

    // Imagen legacy JSON
    evento.setImagenPortadaUrl(dto.getImagenPortadaUrl());
    evento.setImagenPortadaContentType(dto.getImagenPortadaContentType());
    evento.setImagenPortadaPublicId(null);

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
      evento.setLatitud(null);
      evento.setLongitud(null);
    } else {
      evento.setUrlVirtual(null);
      evento.setUbicacion(dto.getUbicacion());
      evento.setNombreLugar(dto.getNombreLugar());
      evento.setDireccionCompleta(dto.getDireccionCompleta());
      evento.setCiudad(dto.getCiudad());
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
  }

  private void mapearFormEnEvento(CrearEventoForm form, Evento evento) {
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
  }

  private List<MultipartFile> normalizarImagenes(List<MultipartFile> imagenes) {
    List<MultipartFile> resultado = new ArrayList<>();
    if (imagenes == null || imagenes.isEmpty()) {
      return resultado;
    }

    for (MultipartFile imagen : imagenes) {
      if (imagen != null && !imagen.isEmpty()) {
        resultado.add(imagen);
      }
    }

    return resultado;
  }

  private void validarCantidadImagenes(List<MultipartFile> imagenes, EstadoEvento estado) {
    int cantidad = imagenes.size();

    if (cantidad > MAX_IMAGENES) {
      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Puedes subir máximo " + MAX_IMAGENES + " imágenes por evento"
      );
    }

    boolean esBorrador = EstadoEvento.borrador.equals(estado);

    if (!esBorrador && cantidad < MIN_IMAGENES_PUBLICADO) {
      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Debes subir al menos 1 imagen para publicar el evento"
      );
    }
  }

  private void procesarImagenes(Evento evento, List<MultipartFile> imagenes, List<String> publicIdsSubidos) {
    evento.limpiarImagenes();

    if (imagenes == null || imagenes.isEmpty()) {
      evento.setImagenPortadaUrl(null);
      evento.setImagenPortadaContentType(null);
      evento.setImagenPortadaPublicId(null);
      return;
    }

    for (int i = 0; i < imagenes.size(); i++) {
      MultipartFile imagen = imagenes.get(i);
      ImagenCloudinarySubida subida = subirImagenACloudinary(imagen);

      publicIdsSubidos.add(subida.publicId());

      EventoImagen eventoImagen = new EventoImagen();
      eventoImagen.setImageUrl(subida.secureUrl());
      eventoImagen.setPublicId(subida.publicId());
      eventoImagen.setOrden(i);
      eventoImagen.setEsPrincipal(i == 0);

      evento.agregarImagen(eventoImagen);

      if (i == 0) {
        evento.setImagenPortadaUrl(subida.secureUrl());
        evento.setImagenPortadaContentType(subida.contentType());
        evento.setImagenPortadaPublicId(subida.publicId());
      }
    }
  }

  private void procesarRedesSociales(CrearEventoForm form, Evento evento) {
    evento.limpiarRedesSociales();

    if (form.getRedesSociales() == null || form.getRedesSociales().isEmpty()) {
      return;
    }

    int orden = 0;
    for (RedSocialEventoForm redForm : form.getRedesSociales()) {
      if (redForm == null) {
        continue;
      }

      if (redForm.getPlataforma() == null || !StringUtils.hasText(redForm.getUrl())) {
        continue;
      }

      EventoRedSocial red = new EventoRedSocial();
      red.setPlataforma(redForm.getPlataforma());
      red.setUrl(redForm.getUrl().trim());
      red.setOrden(orden++);

      evento.agregarRedSocial(red);
    }
  }

  private ImagenCloudinarySubida subirImagenACloudinary(MultipartFile file) {
    try {
      Map<?, ?> resultado = cloudinary.uploader().upload(
              file.getBytes(),
              ObjectUtils.asMap(
                      "folder", CLOUDINARY_FOLDER_EVENTOS,
                      "resource_type", "image"
              )
      );

      String secureUrl = (String) resultado.get("secure_url");
      String publicId = (String) resultado.get("public_id");
      String contentType = file.getContentType();

      if (!StringUtils.hasText(secureUrl) || !StringUtils.hasText(publicId)) {
        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Cloudinary no devolvió una URL o public_id válidos"
        );
      }

      return new ImagenCloudinarySubida(secureUrl, publicId, contentType);

    } catch (IOException e) {
      throw new ResponseStatusException(
              HttpStatus.INTERNAL_SERVER_ERROR,
              "No se pudo subir una de las imágenes del evento"
      );
    }
  }

  private void eliminarImagenesDeCloudinary(Evento evento) {
    Set<String> publicIds = new LinkedHashSet<>();

    if (StringUtils.hasText(evento.getImagenPortadaPublicId())) {
      publicIds.add(evento.getImagenPortadaPublicId());
    }

    if (evento.getImagenes() != null) {
      for (EventoImagen imagen : evento.getImagenes()) {
        if (imagen != null && StringUtils.hasText(imagen.getPublicId())) {
          publicIds.add(imagen.getPublicId());
        }
      }
    }

    for (String publicId : publicIds) {
      eliminarImagenDeCloudinary(publicId);
    }
  }

  private void eliminarImagenDeCloudinary(String publicId) {
    if (!StringUtils.hasText(publicId)) {
      return;
    }

    try {
      cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    } catch (Exception ignored) {
      // Si falla el borrado remoto, no bloqueamos el flujo de negocio.
    }
  }

  private void limpiarSubidasFallidas(List<String> publicIdsSubidos) {
    if (publicIdsSubidos == null || publicIdsSubidos.isEmpty()) {
      return;
    }

    for (String publicId : publicIdsSubidos) {
      eliminarImagenDeCloudinary(publicId);
    }
  }

  private record ImagenCloudinarySubida(String secureUrl, String publicId, String contentType) {
  }

  @Override
  @Transactional
  public Evento actualizarEventoYNotificar(Integer idEvento, Evento cambios) {
    Evento existente = eventoRepository.findById(idEvento)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));

    boolean cambioClave =
            (cambios.getTitulo() != null && !cambios.getTitulo().equals(existente.getTitulo())) ||
                    (cambios.getFecha() != null && !cambios.getFecha().equals(existente.getFecha())) ||
                    (cambios.getUbicacion() != null && !cambios.getUbicacion().equals(existente.getUbicacion()));

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
        notificacionService.crearNotificacionConReferencia(
                ins.getUsuario(),
                "El evento \"" + actualizado.getTitulo() + "\" fue actualizado. Revisa los cambios.",
                "EVENTO",
                actualizado.getIdEvento()
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
      notificacionService.crearNotificacionConReferencia(
              ins.getUsuario(),
              "El evento \"" + evento.getTitulo() + "\" fue eliminado/cancelado. Tu inscripción quedó anulada.",
              "EVENTO_ELIMINADO",
              evento.getIdEvento()
      );
    }

    eliminarImagenesDeCloudinary(evento);
    eventoCommentRepository.deleteByEvento_IdEvento(idEvento);
    eventoRepository.delete(evento);
  }
}