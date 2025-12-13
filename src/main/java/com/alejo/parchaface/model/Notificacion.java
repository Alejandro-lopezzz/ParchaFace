package com.alejo.parchaface.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_notificacion;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String mensaje;

    @Column(nullable = false)
    private LocalDateTime fecha_envio = LocalDateTime.now();

    @Column(nullable = false)
    private Boolean leido = false;

    // Getters y setters
    public Integer getId_notificacion() { return id_notificacion; }
    public void setId_notificacion(Integer id_notificacion) { this.id_notificacion = id_notificacion; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public LocalDateTime getFecha_envio() { return fecha_envio; }
    public void setFecha_envio(LocalDateTime fecha_envio) { this.fecha_envio = fecha_envio; }

    public Boolean getLeido() { return leido; }
    public void setLeido(Boolean leido) { this.leido = leido; }
}
