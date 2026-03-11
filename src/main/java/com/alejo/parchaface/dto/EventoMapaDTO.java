package com.alejo.parchaface.dto;

import java.time.LocalDateTime;

public class EventoMapaDTO {

    private Integer idEvento;
    private String titulo;
    private String categoria;
    private LocalDateTime fecha;
    private String ciudad;
    private String nombreLugar;
    private String imagenPortadaUrl;
    private Double latitud;
    private Double longitud;

    public EventoMapaDTO() {
    }

    public EventoMapaDTO(
            Integer idEvento,
            String titulo,
            String categoria,
            LocalDateTime fecha,
            String ciudad,
            String nombreLugar,
            String imagenPortadaUrl,
            Double latitud,
            Double longitud
    ) {
        this.idEvento = idEvento;
        this.titulo = titulo;
        this.categoria = categoria;
        this.fecha = fecha;
        this.ciudad = ciudad;
        this.nombreLugar = nombreLugar;
        this.imagenPortadaUrl = imagenPortadaUrl;
        this.latitud = latitud;
        this.longitud = longitud;
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

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getNombreLugar() {
        return nombreLugar;
    }

    public void setNombreLugar(String nombreLugar) {
        this.nombreLugar = nombreLugar;
    }

    public String getImagenPortadaUrl() {
        return imagenPortadaUrl;
    }

    public void setImagenPortadaUrl(String imagenPortadaUrl) {
        this.imagenPortadaUrl = imagenPortadaUrl;
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
}