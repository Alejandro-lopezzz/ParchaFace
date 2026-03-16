package com.alejo.parchaface.dto;

import java.util.ArrayList;
import java.util.List;

public class PerfilUsuarioDto {

    private Integer idUsuario;
    private String nombre;
    private String correo;
    private String fotoPerfil;
    private String fotoPortada;
    private String acercaDe;
    private List<String> categoriasPreferidas = new ArrayList<>();

    private long totalSeguidores;
    private long totalSiguiendo;
    private boolean seguidoPorMi;
    private boolean esMiPerfil;

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

    public String getFotoPortada() {
        return fotoPortada;
    }

    public void setFotoPortada(String fotoPortada) {
        this.fotoPortada = fotoPortada;
    }

    public String getAcercaDe() {
        return acercaDe;
    }

    public void setAcercaDe(String acercaDe) {
        this.acercaDe = acercaDe;
    }

    public List<String> getCategoriasPreferidas() {
        return categoriasPreferidas;
    }

    public void setCategoriasPreferidas(List<String> categoriasPreferidas) {
        this.categoriasPreferidas = categoriasPreferidas != null ? categoriasPreferidas : new ArrayList<>();
    }

    public long getTotalSeguidores() {
        return totalSeguidores;
    }

    public void setTotalSeguidores(long totalSeguidores) {
        this.totalSeguidores = totalSeguidores;
    }

    public long getTotalSiguiendo() {
        return totalSiguiendo;
    }

    public void setTotalSiguiendo(long totalSiguiendo) {
        this.totalSiguiendo = totalSiguiendo;
    }

    public boolean isSeguidoPorMi() {
        return seguidoPorMi;
    }

    public void setSeguidoPorMi(boolean seguidoPorMi) {
        this.seguidoPorMi = seguidoPorMi;
    }

    public boolean isEsMiPerfil() {
        return esMiPerfil;
    }

    public void setEsMiPerfil(boolean esMiPerfil) {
        this.esMiPerfil = esMiPerfil;
    }
}