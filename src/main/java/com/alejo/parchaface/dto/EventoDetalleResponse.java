package com.alejo.parchaface.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class EventoDetalleResponse {

  private Integer idEvento;
  private String titulo;
  private String descripcion;
  private String categoria;
  private String imagenPortadaUrl;

  private LocalDateTime fecha;
  private LocalTime horaInicio;
  private LocalTime horaFin;

  private Boolean eventoEnLinea;
  private String urlVirtual;

  private String ubicacion;
  private String nombreLugar;
  private String direccionCompleta;
  private String ciudad;
  private Double latitud;
  private Double longitud;

  private Integer cupo;
  private Boolean eventoGratuito;
  private BigDecimal precio;

  private String emailContacto;
  private String telefonoContacto;
  private String sitioWeb;

  private Boolean eventoPublico;
  private String detallePrivado;
  private Boolean permitirComentarios;
  private Boolean recordatoriosAutomaticos;

  private String estadoEvento;

  private Integer idOrganizador;
  private String nombreOrganizador;
  private String correoOrganizador;

  public EventoDetalleResponse() {
  }

  public EventoDetalleResponse(
    Integer idEvento,
    String titulo,
    String descripcion,
    String categoria,
    String imagenPortadaUrl,
    LocalDateTime fecha,
    LocalTime horaInicio,
    LocalTime horaFin,
    Boolean eventoEnLinea,
    String urlVirtual,
    String ubicacion,
    String nombreLugar,
    String direccionCompleta,
    String ciudad,
    Double latitud,
    Double longitud,
    Integer cupo,
    Boolean eventoGratuito,
    BigDecimal precio,
    String emailContacto,
    String telefonoContacto,
    String sitioWeb,
    Boolean eventoPublico,
    String detallePrivado,
    Boolean permitirComentarios,
    Boolean recordatoriosAutomaticos,
    String estadoEvento,
    Integer idOrganizador,
    String nombreOrganizador,
    String correoOrganizador
  ) {
    this.idEvento = idEvento;
    this.titulo = titulo;
    this.descripcion = descripcion;
    this.categoria = categoria;
    this.imagenPortadaUrl = imagenPortadaUrl;
    this.fecha = fecha;
    this.horaInicio = horaInicio;
    this.horaFin = horaFin;
    this.eventoEnLinea = eventoEnLinea;
    this.urlVirtual = urlVirtual;
    this.ubicacion = ubicacion;
    this.nombreLugar = nombreLugar;
    this.direccionCompleta = direccionCompleta;
    this.ciudad = ciudad;
    this.latitud = latitud;
    this.longitud = longitud;
    this.cupo = cupo;
    this.eventoGratuito = eventoGratuito;
    this.precio = precio;
    this.emailContacto = emailContacto;
    this.telefonoContacto = telefonoContacto;
    this.sitioWeb = sitioWeb;
    this.eventoPublico = eventoPublico;
    this.detallePrivado = detallePrivado;
    this.permitirComentarios = permitirComentarios;
    this.recordatoriosAutomaticos = recordatoriosAutomaticos;
    this.estadoEvento = estadoEvento;
    this.idOrganizador = idOrganizador;
    this.nombreOrganizador = nombreOrganizador;
    this.correoOrganizador = correoOrganizador;
  }

  public Integer getIdEvento() {
    return idEvento;
  }

  public void setIdEvento(Integer idEvento) {
    this.idEvento = idEvento;
  }

  public String getTitulo() {
    return titulo;
  }

  public void setTitulo(String titulo) {
    this.titulo = titulo;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public String getCategoria() {
    return categoria;
  }

  public void setCategoria(String categoria) {
    this.categoria = categoria;
  }

  public String getImagenPortadaUrl() {
    return imagenPortadaUrl;
  }

  public void setImagenPortadaUrl(String imagenPortadaUrl) {
    this.imagenPortadaUrl = imagenPortadaUrl;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public void setFecha(LocalDateTime fecha) {
    this.fecha = fecha;
  }

  public LocalTime getHoraInicio() {
    return horaInicio;
  }

  public void setHoraInicio(LocalTime horaInicio) {
    this.horaInicio = horaInicio;
  }

  public LocalTime getHoraFin() {
    return horaFin;
  }

  public void setHoraFin(LocalTime horaFin) {
    this.horaFin = horaFin;
  }

  public Boolean getEventoEnLinea() {
    return eventoEnLinea;
  }

  public void setEventoEnLinea(Boolean eventoEnLinea) {
    this.eventoEnLinea = eventoEnLinea;
  }

  public String getUrlVirtual() {
    return urlVirtual;
  }

  public void setUrlVirtual(String urlVirtual) {
    this.urlVirtual = urlVirtual;
  }

  public String getUbicacion() {
    return ubicacion;
  }

  public void setUbicacion(String ubicacion) {
    this.ubicacion = ubicacion;
  }

  public String getNombreLugar() {
    return nombreLugar;
  }

  public void setNombreLugar(String nombreLugar) {
    this.nombreLugar = nombreLugar;
  }

  public String getDireccionCompleta() {
    return direccionCompleta;
  }

  public void setDireccionCompleta(String direccionCompleta) {
    this.direccionCompleta = direccionCompleta;
  }

  public String getCiudad() {
    return ciudad;
  }

  public void setCiudad(String ciudad) {
    this.ciudad = ciudad;
  }

  public Double getLatitud() {
    return latitud;
  }

  public void setLatitud(Double latitud) {
    this.latitud = latitud;
  }

  public Double getLongitud() {
    return longitud;
  }

  public void setLongitud(Double longitud) {
    this.longitud = longitud;
  }

  public Integer getCupo() {
    return cupo;
  }

  public void setCupo(Integer cupo) {
    this.cupo = cupo;
  }

  public Boolean getEventoGratuito() {
    return eventoGratuito;
  }

  public void setEventoGratuito(Boolean eventoGratuito) {
    this.eventoGratuito = eventoGratuito;
  }

  public BigDecimal getPrecio() {
    return precio;
  }

  public void setPrecio(BigDecimal precio) {
    this.precio = precio;
  }

  public String getEmailContacto() {
    return emailContacto;
  }

  public void setEmailContacto(String emailContacto) {
    this.emailContacto = emailContacto;
  }

  public String getTelefonoContacto() {
    return telefonoContacto;
  }

  public void setTelefonoContacto(String telefonoContacto) {
    this.telefonoContacto = telefonoContacto;
  }

  public String getSitioWeb() {
    return sitioWeb;
  }

  public void setSitioWeb(String sitioWeb) {
    this.sitioWeb = sitioWeb;
  }

  public Boolean getEventoPublico() {
    return eventoPublico;
  }

  public void setEventoPublico(Boolean eventoPublico) {
    this.eventoPublico = eventoPublico;
  }

  public String getDetallePrivado() {
    return detallePrivado;
  }

  public void setDetallePrivado(String detallePrivado) {
    this.detallePrivado = detallePrivado;
  }

  public Boolean getPermitirComentarios() {
    return permitirComentarios;
  }

  public void setPermitirComentarios(Boolean permitirComentarios) {
    this.permitirComentarios = permitirComentarios;
  }

  public Boolean getRecordatoriosAutomaticos() {
    return recordatoriosAutomaticos;
  }

  public void setRecordatoriosAutomaticos(Boolean recordatoriosAutomaticos) {
    this.recordatoriosAutomaticos = recordatoriosAutomaticos;
  }

  public String getEstadoEvento() {
    return estadoEvento;
  }

  public void setEstadoEvento(String estadoEvento) {
    this.estadoEvento = estadoEvento;
  }

  public Integer getIdOrganizador() {
    return idOrganizador;
  }

  public void setIdOrganizador(Integer idOrganizador) {
    this.idOrganizador = idOrganizador;
  }

  public String getNombreOrganizador() {
    return nombreOrganizador;
  }

  public void setNombreOrganizador(String nombreOrganizador) {
    this.nombreOrganizador = nombreOrganizador;
  }

  public String getCorreoOrganizador() {
    return correoOrganizador;
  }

  public void setCorreoOrganizador(String correoOrganizador) {
    this.correoOrganizador = correoOrganizador;
  }
}
