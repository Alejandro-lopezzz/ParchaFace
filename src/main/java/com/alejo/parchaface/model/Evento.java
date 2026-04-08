package com.alejo.parchaface.model;

import com.alejo.parchaface.model.enums.EstadoEvento;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evento")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Integer idEvento;

    // ======================
    // Básico
    // ======================
    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "categoria", length = 80)
    private String categoria;

    /**
     * Compatibilidad temporal con el front actual.
     * Esta seguirá siendo la imagen principal/portada del evento.
     */
    @Column(name = "imagen_portada_url", length = 500)
    private String imagenPortadaUrl;

    /**
     * Tipo MIME de la portada principal.
     * Puedes mantenerlo por compatibilidad mientras migras completamente a Cloudinary.
     */
    @Column(name = "imagen_portada_content_type", length = 80)
    private String imagenPortadaContentType;

    /**
     * Nuevo: public_id de Cloudinary para la portada principal.
     * Sirve para reemplazar o borrar la imagen después.
     */
    @Column(name = "imagen_portada_public_id", length = 255)
    private String imagenPortadaPublicId;

    // ======================
    // Fecha / horas
    // ======================
    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "hora", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    // ======================
    // Modalidad
    // ======================
    @Column(name = "evento_en_linea", nullable = false)
    private Boolean eventoEnLinea = false;

    @Column(name = "url_virtual", length = 500)
    private String urlVirtual;

    // ======================
    // Ubicación (presencial)
    // ======================
    @Column(name = "lugar", length = 200)
    private String ubicacion;

    @Column(name = "nombre_lugar", length = 200)
    private String nombreLugar;

    @Column(name = "direccion_completa", length = 300)
    private String direccionCompleta;

    @Column(name = "ciudad", length = 120)
    private String ciudad;

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    // ======================
    // Cupo / Precio
    // ======================
    @Column(name = "cupo")
    private Integer cupo = 0;

    @Column(name = "evento_gratuito", nullable = false)
    private Boolean eventoGratuito = true;

    @Column(name = "precio", precision = 10, scale = 2)
    private BigDecimal precio;

    // ======================
    // Contacto
    // ======================
    @Column(name = "email_contacto", length = 150)
    private String emailContacto;

    @Column(name = "telefono_contacto", length = 30)
    private String telefonoContacto;

    @Column(name = "sitio_web", length = 500)
    private String sitioWeb;

    // ======================
    // Privacidad / config
    // ======================
    @Column(name = "evento_publico", nullable = false)
    private Boolean eventoPublico = true;

    @Column(name = "detalle_privado", length = 200)
    private String detallePrivado;

    @Column(name = "permitir_comentarios", nullable = false)
    private Boolean permitirComentarios = true;

    @Column(name = "recordatorios_automaticos", nullable = false)
    private Boolean recordatoriosAutomaticos = false;

    // ======================
    // Auditoría / estado
    // ======================
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_evento", nullable = false)
    private EstadoEvento estadoEvento;

    // ======================
    // Relaciones
    // ======================
    @ManyToOne
    @JoinColumn(name = "id_organizador", nullable = false)
    private Usuario organizador;

    @JsonIgnore
    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inscripcion> inscripciones = new ArrayList<>();

    @Transient
    private Long totalInscritos = 0L;

    /**
     * Nueva relación: múltiples imágenes por evento (máx 3 por lógica de negocio).
     */
    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC, idEventoImagen ASC")
    private List<EventoImagen> imagenes = new ArrayList<>();

    /**
     * Nueva relación: redes sociales opcionales del evento.
     */
    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC, idEventoRedSocial ASC")
    private List<EventoRedSocial> redesSociales = new ArrayList<>();

    // ======================
    // Hooks
    // ======================
    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) this.fechaCreacion = LocalDateTime.now();
        if (this.estadoEvento == null) this.estadoEvento = EstadoEvento.activo;

        if (this.eventoEnLinea == null) this.eventoEnLinea = false;
        if (this.eventoGratuito == null) this.eventoGratuito = true;
        if (this.eventoPublico == null) this.eventoPublico = true;
        if (this.permitirComentarios == null) this.permitirComentarios = true;
        if (this.recordatoriosAutomaticos == null) this.recordatoriosAutomaticos = false;
    }

    // ======================
    // Helpers imágenes
    // ======================
    public void agregarImagen(EventoImagen imagen) {
        if (imagen == null) return;
        imagen.setEvento(this);
        this.imagenes.add(imagen);
    }

    public void removerImagen(EventoImagen imagen) {
        if (imagen == null) return;
        this.imagenes.remove(imagen);
        imagen.setEvento(null);
    }

    public void limpiarImagenes() {
        for (EventoImagen imagen : this.imagenes) {
            imagen.setEvento(null);
        }
        this.imagenes.clear();
    }

    // ======================
    // Helpers redes
    // ======================
    public void agregarRedSocial(EventoRedSocial redSocial) {
        if (redSocial == null) return;
        redSocial.setEvento(this);
        this.redesSociales.add(redSocial);
    }

    public void removerRedSocial(EventoRedSocial redSocial) {
        if (redSocial == null) return;
        this.redesSociales.remove(redSocial);
        redSocial.setEvento(null);
    }

    public void limpiarRedesSociales() {
        for (EventoRedSocial redSocial : this.redesSociales) {
            redSocial.setEvento(null);
        }
        this.redesSociales.clear();
    }

    // ======================
    // Getters / Setters
    // ======================
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

    public String getImagenPortadaContentType() {
        return imagenPortadaContentType;
    }

    public void setImagenPortadaContentType(String imagenPortadaContentType) {
        this.imagenPortadaContentType = imagenPortadaContentType;
    }

    public String getImagenPortadaPublicId() {
        return imagenPortadaPublicId;
    }

    public void setImagenPortadaPublicId(String imagenPortadaPublicId) {
        this.imagenPortadaPublicId = imagenPortadaPublicId;
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

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public EstadoEvento getEstadoEvento() {
        return estadoEvento;
    }

    public void setEstadoEvento(EstadoEvento estadoEvento) {
        this.estadoEvento = estadoEvento;
    }

    public Usuario getOrganizador() {
        return organizador;
    }

    public void setOrganizador(Usuario organizador) {
        this.organizador = organizador;
    }

    public List<Inscripcion> getInscripciones() {
        return inscripciones;
    }

    public void setInscripciones(List<Inscripcion> inscripciones) {
        this.inscripciones = inscripciones;
    }

    public List<EventoImagen> getImagenes() {
        return imagenes;
    }

    public void setImagenes(List<EventoImagen> imagenes) {
        this.imagenes = imagenes;
    }

    public List<EventoRedSocial> getRedesSociales() {
        return redesSociales;
    }

    public void setRedesSociales(List<EventoRedSocial> redesSociales) {
        this.redesSociales = redesSociales;
    }

    public Long getTotalInscritos() {
        return totalInscritos;
    }

    public void setTotalInscritos(Long totalInscritos) {
        this.totalInscritos = totalInscritos;
    }
}