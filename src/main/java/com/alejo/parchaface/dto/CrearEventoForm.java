package com.alejo.parchaface.dto;

import com.alejo.parchaface.model.enums.PlataformaSocial;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CrearEventoForm {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    private String titulo;

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    private String descripcion;

    @Size(max = 80, message = "La categoría no puede superar los 80 caracteres")
    private String categoria;

    // Ahora soporta varias imágenes
    @Size(max = 3, message = "Puedes subir máximo 3 imágenes")
    private List<MultipartFile> imagenes = new ArrayList<>();

    // Redes sociales opcionales
    @Valid
    private List<RedSocialEventoForm> redesSociales = new ArrayList<>();

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de finalización es obligatoria")
    private LocalTime horaFin;

    @NotNull(message = "Debes indicar si el evento es en línea o no")
    private Boolean eventoEnLinea;

    @Size(max = 500, message = "La URL virtual no puede superar los 500 caracteres")
    private String urlVirtual;

    @Size(max = 200, message = "La ubicación no puede superar los 200 caracteres")
    private String ubicacion;

    @Size(max = 200, message = "El nombre del lugar no puede superar los 200 caracteres")
    private String nombreLugar;

    @Size(max = 300, message = "La dirección completa no puede superar los 300 caracteres")
    private String direccionCompleta;

    @Size(max = 120, message = "La ciudad no puede superar los 120 caracteres")
    private String ciudad;

    private Double latitud;
    private Double longitud;

    @NotNull(message = "El cupo es obligatorio")
    @Min(value = 1, message = "El cupo debe ser mayor a 0")
    private Integer cupo;

    @NotNull(message = "Debes indicar si el evento es gratuito o no")
    private Boolean eventoGratuito;

    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    private BigDecimal precio;

    @Email(message = "Email de contacto inválido")
    @Size(max = 150, message = "El email de contacto no puede superar los 150 caracteres")
    private String emailContacto;

    @Size(max = 30, message = "El teléfono de contacto no puede superar los 30 caracteres")
    private String telefonoContacto;

    @Size(max = 500, message = "El sitio web no puede superar los 500 caracteres")
    private String sitioWeb;

    @NotNull(message = "Debes indicar si el evento es público o no")
    private Boolean eventoPublico;

    @Size(max = 200, message = "El detalle privado no puede superar los 200 caracteres")
    private String detallePrivado;

    @NotNull(message = "Debes indicar si permites comentarios o no")
    private Boolean permitirComentarios;

    @NotNull(message = "Debes indicar si envías recordatorios automáticos o no")
    private Boolean recordatoriosAutomaticos;

    // ======================
    // VALIDACIONES CONDICIONALES
    // ======================

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

    @AssertTrue(message = "Si el evento NO es en línea, debes enviar latitud y longitud")
    public boolean isCoordenadasValidas() {
        if (Boolean.FALSE.equals(eventoEnLinea)) {
            return latitud != null && longitud != null;
        }
        return true;
    }

    @AssertTrue(message = "Cada imagen debe ser JPG, JPEG, PNG o WEBP")
    public boolean isImagenesValidas() {
        if (imagenes == null || imagenes.isEmpty()) return true;

        for (MultipartFile imagen : imagenes) {
            if (imagen == null || imagen.isEmpty()) {
                continue;
            }

            String ct = imagen.getContentType();
            if (ct == null) return false;

            ct = ct.toLowerCase().trim();
            boolean valida = ct.equals("image/jpeg")
                    || ct.equals("image/jpg")
                    || ct.equals("image/png")
                    || ct.equals("image/webp");

            if (!valida) return false;
        }
        return true;
    }

    @AssertTrue(message = "No puedes repetir plataformas de redes sociales en el mismo evento")
    public boolean isRedesSocialesValidas() {
        if (redesSociales == null || redesSociales.isEmpty()) return true;

        Set<PlataformaSocial> plataformas = new HashSet<>();
        for (RedSocialEventoForm red : redesSociales) {
            if (red == null || red.getPlataforma() == null) {
                continue;
            }
            if (!plataformas.add(red.getPlataforma())) {
                return false;
            }
        }
        return true;
    }

    // ======================
    // GETTERS / SETTERS
    // ======================

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

    public List<MultipartFile> getImagenes() {
        return imagenes;
    }

    public void setImagenes(List<MultipartFile> imagenes) {
        this.imagenes = imagenes;
    }

    public List<RedSocialEventoForm> getRedesSociales() {
        return redesSociales;
    }

    public void setRedesSociales(List<RedSocialEventoForm> redesSociales) {
        this.redesSociales = redesSociales;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
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
}