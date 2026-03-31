package com.alejo.parchaface.model;

import com.alejo.parchaface.model.enums.Estado;
import com.alejo.parchaface.model.enums.Rol;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 100)
    private String correo;

    @Column(name = "contrasena", nullable = false)
    @JsonIgnore
    private String contrasena;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol = Rol.USUARIO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.ACTIVO;

    @JsonIgnore
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inscripcion> inscripciones = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notificacion> notificaciones = new ArrayList<>();

    @Column(name = "preferencias_completadas", nullable = false)
    private Boolean preferenciasCompletadas = false;

    @ElementCollection(fetch = LAZY)
    @CollectionTable(name = "usuario_categorias_preferidas", joinColumns = @JoinColumn(name = "id_usuario"))
    @Column(name = "categoria")
    private List<String> categoriasPreferidas = new ArrayList<>();

    @Column(name = "acerca_de", length = 700)
    private String acercaDe;

    @Column(name = "google_sub", unique = true, length = 100)
    private String googleSub;

    @Column(name = "auth_provider", nullable = false, length = 20)
    private String authProvider = "LOCAL";

    @ElementCollection(fetch = LAZY)
    @CollectionTable(name = "usuario_redes_sociales", joinColumns = @JoinColumn(name = "id_usuario"))
    private List<RedSocial> redesSociales = new ArrayList<>();

    @Column(name = "foto_perfil_url", length = 1000)
    private String fotoPerfilUrl;

    @Column(name = "foto_perfil_public_id", length = 255)
    private String fotoPerfilPublicId;

    @Column(name = "foto_portada_url", length = 1000)
    private String fotoPortadaUrl;

    @Column(name = "foto_portada_public_id", length = 255)
    private String fotoPortadaPublicId;

    @JsonIgnore
    @OneToMany(mappedBy = "seguidor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seguimiento> siguiendo = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "seguido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seguimiento> seguidores = new ArrayList<>();

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

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public List<Inscripcion> getInscripciones() {
        return inscripciones;
    }

    public void setInscripciones(List<Inscripcion> inscripciones) {
        this.inscripciones = inscripciones != null ? inscripciones : new ArrayList<>();
    }

    public List<Notificacion> getNotificaciones() {
        return notificaciones;
    }

    public void setNotificaciones(List<Notificacion> notificaciones) {
        this.notificaciones = notificaciones != null ? notificaciones : new ArrayList<>();
    }

    public Boolean getPreferenciasCompletadas() {
        return preferenciasCompletadas;
    }

    public void setPreferenciasCompletadas(Boolean preferenciasCompletadas) {
        this.preferenciasCompletadas = preferenciasCompletadas != null ? preferenciasCompletadas : false;
    }

    public List<String> getCategoriasPreferidas() {
        return categoriasPreferidas;
    }

    public void setCategoriasPreferidas(List<String> categoriasPreferidas) {
        this.categoriasPreferidas = categoriasPreferidas != null ? categoriasPreferidas : new ArrayList<>();
    }

    public String getAcercaDe() {
        return acercaDe;
    }

    public void setAcercaDe(String acercaDe) {
        this.acercaDe = acercaDe;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public void setGoogleSub(String googleSub) {
        this.googleSub = googleSub;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public List<RedSocial> getRedesSociales() {
        return redesSociales;
    }

    public void setRedesSociales(List<RedSocial> redesSociales) {
        this.redesSociales = redesSociales != null ? redesSociales : new ArrayList<>();
    }

    public String getFotoPerfilUrl() {
        return fotoPerfilUrl;
    }

    public void setFotoPerfilUrl(String fotoPerfilUrl) {
        this.fotoPerfilUrl = fotoPerfilUrl;
    }

    public String getFotoPerfilPublicId() {
        return fotoPerfilPublicId;
    }

    public void setFotoPerfilPublicId(String fotoPerfilPublicId) {
        this.fotoPerfilPublicId = fotoPerfilPublicId;
    }

    public String getFotoPortadaUrl() {
        return fotoPortadaUrl;
    }

    public void setFotoPortadaUrl(String fotoPortadaUrl) {
        this.fotoPortadaUrl = fotoPortadaUrl;
    }

    public String getFotoPortadaPublicId() {
        return fotoPortadaPublicId;
    }

    public void setFotoPortadaPublicId(String fotoPortadaPublicId) {
        this.fotoPortadaPublicId = fotoPortadaPublicId;
    }

    public List<Seguimiento> getSiguiendo() {
        return siguiendo;
    }

    public void setSiguiendo(List<Seguimiento> siguiendo) {
        this.siguiendo = siguiendo != null ? siguiendo : new ArrayList<>();
    }

    public List<Seguimiento> getSeguidores() {
        return seguidores;
    }

    public void setSeguidores(List<Seguimiento> seguidores) {
        this.seguidores = seguidores != null ? seguidores : new ArrayList<>();
    }

    // Compatibilidad con el frontend/código existente
    public String getFotoPerfil() {
        return fotoPerfilUrl;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfilUrl = fotoPerfil;
    }

    public String getFotoPortada() {
        return fotoPortadaUrl;
    }

    public void setFotoPortada(String fotoPortada) {
        this.fotoPortadaUrl = fotoPortada;
    }
}