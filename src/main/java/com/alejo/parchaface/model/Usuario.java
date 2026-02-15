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

    // Nunca debe salir en JSON
    @Column(name = "contrasena", nullable = false)
    @JsonIgnore
    private String contrasena;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol = Rol.USUARIO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.ACTIVO;

    // Evita JSON enorme y posibles ciclos al serializar relaciones
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

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public List<Inscripcion> getInscripciones() { return inscripciones; }
    public void setInscripciones(List<Inscripcion> inscripciones) { this.inscripciones = inscripciones; }

    public List<Notificacion> getNotificaciones() { return notificaciones; }
    public void setNotificaciones(List<Notificacion> notificaciones) { this.notificaciones = notificaciones; }

    public Boolean getPreferenciasCompletadas() { return preferenciasCompletadas; }
    public void setPreferenciasCompletadas(Boolean preferenciasCompletadas) { this.preferenciasCompletadas = preferenciasCompletadas != null ? preferenciasCompletadas : false; }

    public List<String> getCategoriasPreferidas() { return categoriasPreferidas; }
    public void setCategoriasPreferidas(List<String> categoriasPreferidas) { this.categoriasPreferidas = categoriasPreferidas != null ? categoriasPreferidas : new ArrayList<>(); }
}
