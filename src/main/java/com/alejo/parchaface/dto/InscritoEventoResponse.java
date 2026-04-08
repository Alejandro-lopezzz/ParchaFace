package com.alejo.parchaface.dto;

import java.time.LocalDate;

public class InscritoEventoResponse {

    private Integer idUsuario;
    private String nombre;
    private String correo;
    private String fotoPerfil;
    private String acercaDe;
    private LocalDate fechaInscripcion;

    public InscritoEventoResponse() {
    }

    public InscritoEventoResponse(
            Integer idUsuario,
            String nombre,
            String correo,
            String fotoPerfil,
            String acercaDe,
            LocalDate fechaInscripcion
    ) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.correo = correo;
        this.fotoPerfil = fotoPerfil;
        this.acercaDe = acercaDe;
        this.fechaInscripcion = fechaInscripcion;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }

    public String getAcercaDe() {
        return acercaDe;
    }

    public void setAcercaDe(String acercaDe) {
        this.acercaDe = acercaDe;
    }

    public LocalDate getFechaInscripcion() {
        return fechaInscripcion;
    }

    public void setFechaInscripcion(LocalDate fechaInscripcion) {
        this.fechaInscripcion = fechaInscripcion;
    }
}