package com.alejo.parchaface.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class CrearEventoDTO {

    // ======================
    // Básico
    // ======================
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150)
    private String titulo;

    @Size(max = 500)
    private String descripcion;

    @Size(max = 80)
    private String categoria;

    // Imagen de portada (por ahora como URL + contentType; luego lo pasamos a MultipartFile si quieres)
    @Size(max = 500)
    private String imagenPortadaUrl;

    @Size(max = 80)
    private String imagenPortadaContentType; // ej: image/jpeg

    // ======================
    // Fecha y horas
    // ======================
    @NotNull(message = "La fecha es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio es obligatoria")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de finalización es obligatoria")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime horaFin;

    // ======================
    // Modalidad
    // ======================
    @NotNull(message = "Debes indicar si el evento es en línea o no")
    private Boolean eventoEnLinea; // true/false

    @Size(max = 500)
    private String urlVirtual; // obligatorio si eventoEnLinea = true

    // ======================
    // Ubicación (presencial)
    // ======================
    // NO @NotBlank aquí porque depende de eventoEnLinea
    @Size(max = 200)
    private String ubicacion; // mapea a columna "lugar" en BD

    @Size(max = 200)
    private String nombreLugar;

    @Size(max = 300)
    private String direccionCompleta;

    @Size(max = 120)
    private String ciudad;

    // ======================
    // Cupo / Precio
    // ======================
    @NotNull(message = "El cupo es obligatorio")
    @Min(value = 1, message = "El cupo debe ser mayor a 0")
    private Integer cupo;

    @NotNull(message = "Debes indicar si el evento es gratuito o no")
    private Boolean eventoGratuito;

    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    private BigDecimal precio; // obligatorio si eventoGratuito = false

    // ======================
    // Contacto
    // ======================
    @Email(message = "Email de contacto inválido")
    @Size(max = 150)
    private String emailContacto;

    @Size(max = 30)
    private String telefonoContacto;

    @Size(max = 500)
    private String sitioWeb;

    // ======================
    // Privacidad / Config
    // ======================
    @NotNull(message = "Debes indicar si el evento es público o no")
    private Boolean eventoPublico;

    @Size(max = 200)
    private String detallePrivado; // obligatorio si eventoPublico = false

    @NotNull(message = "Debes indicar si permites comentarios o no")
    private Boolean permitirComentarios;

    @NotNull(message = "Debes indicar si envías recordatorios automáticos o no")
    private Boolean recordatoriosAutomaticos;

    // =========================================================
    // VALIDACIONES CONDICIONALES
    // =========================================================

    @AssertTrue(message = "Si el evento es en línea, debes enviar urlVirtual")
    public boolean isUrlVirtualValida() {
        if (Boolean.TRUE.equals(eventoEnLinea)) {
            return urlVirtual != null && !urlVirtual.isBlank();
        }
        return true;
    }

    @AssertTrue(message = "Si el evento NO es en línea, la ubicación es obligatoria")
    public boolean isUbicacionValida() {
        if (Boolean.FALSE.equals(eventoEnLinea)) {
            return ubicacion != null && !ubicacion.isBlank();
        }
        return true;
    }

    @AssertTrue(message = "Si el evento NO es gratuito, debes enviar el precio")
    public boolean isPrecioValido() {
        if (Boolean.FALSE.equals(eventoGratuito)) {
            return precio != null && precio.compareTo(BigDecimal.ZERO) > 0;
        }
        return true;
    }

    @AssertTrue(message = "Si el evento NO es público, debes indicar el detalle (empresa/índole)")
    public boolean isDetallePrivadoValido() {
        if (Boolean.FALSE.equals(eventoPublico)) {
            return detallePrivado != null && !detallePrivado.isBlank();
        }
        return true;
    }

    @AssertTrue(message = "horaFin debe ser mayor que horaInicio")
    public boolean isRangoHoraValido() {
        if (horaInicio == null || horaFin == null) return true;
        return horaFin.isAfter(horaInicio);
    }

    @AssertTrue(message = "La imagen de portada debe ser JPG (image/jpeg o image/jpg) si envías contentType")
    public boolean isImagenPortadaJpg() {
        if (imagenPortadaContentType == null || imagenPortadaContentType.isBlank()) return true;
        String ct = imagenPortadaContentType.trim().toLowerCase();
        return ct.equals("image/jpeg") || ct.equals("image/jpg");
    }

    // ======================
    // GETTERS / SETTERS (COMPLETOS)
    // ======================

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getImagenPortadaUrl() { return imagenPortadaUrl; }
    public void setImagenPortadaUrl(String imagenPortadaUrl) { this.imagenPortadaUrl = imagenPortadaUrl; }

    public String getImagenPortadaContentType() { return imagenPortadaContentType; }
    public void setImagenPortadaContentType(String imagenPortadaContentType) { this.imagenPortadaContentType = imagenPortadaContentType; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

    public Boolean getEventoEnLinea() { return eventoEnLinea; }
    public void setEventoEnLinea(Boolean eventoEnLinea) { this.eventoEnLinea = eventoEnLinea; }

    public String getUrlVirtual() { return urlVirtual; }
    public void setUrlVirtual(String urlVirtual) { this.urlVirtual = urlVirtual; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getNombreLugar() { return nombreLugar; }
    public void setNombreLugar(String nombreLugar) { this.nombreLugar = nombreLugar; }

    public String getDireccionCompleta() { return direccionCompleta; }
    public void setDireccionCompleta(String direccionCompleta) { this.direccionCompleta = direccionCompleta; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public Integer getCupo() { return cupo; }
    public void setCupo(Integer cupo) { this.cupo = cupo; }

    public Boolean getEventoGratuito() { return eventoGratuito; }
    public void setEventoGratuito(Boolean eventoGratuito) { this.eventoGratuito = eventoGratuito; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public String getEmailContacto() { return emailContacto; }
    public void setEmailContacto(String emailContacto) { this.emailContacto = emailContacto; }

    public String getTelefonoContacto() { return telefonoContacto; }
    public void setTelefonoContacto(String telefonoContacto) { this.telefonoContacto = telefonoContacto; }

    public String getSitioWeb() { return sitioWeb; }
    public void setSitioWeb(String sitioWeb) { this.sitioWeb = sitioWeb; }

    public Boolean getEventoPublico() { return eventoPublico; }
    public void setEventoPublico(Boolean eventoPublico) { this.eventoPublico = eventoPublico; }

    public String getDetallePrivado() { return detallePrivado; }
    public void setDetallePrivado(String detallePrivado) { this.detallePrivado = detallePrivado; }

    public Boolean getPermitirComentarios() { return permitirComentarios; }
    public void setPermitirComentarios(Boolean permitirComentarios) { this.permitirComentarios = permitirComentarios; }

    public Boolean getRecordatoriosAutomaticos() { return recordatoriosAutomaticos; }
    public void setRecordatoriosAutomaticos(Boolean recordatoriosAutomaticos) { this.recordatoriosAutomaticos = recordatoriosAutomaticos; }
}
