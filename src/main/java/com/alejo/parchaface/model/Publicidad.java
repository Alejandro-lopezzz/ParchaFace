package com.alejo.parchaface.model;

import com.alejo.parchaface.model.enums.TipoPublicidad;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "publicidad")
public class Publicidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_publicidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPublicidad tipo;

    @Column(length = 255, nullable = false)
    private String contenido_url;

    @Column(nullable = false)
    private LocalDate fecha_publicacion = LocalDate.now();

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_evento")
    private Evento evento;

    // Getters y setters
    public Integer getId_publicidad() {
        return id_publicidad;
    }

    public void setId_publicidad(Integer id_publicidad) {
        this.id_publicidad = id_publicidad;
    }

    public TipoPublicidad getTipo() {
        return tipo;
    }

    public void setTipo(TipoPublicidad tipo) {
        this.tipo = tipo;
    }

    public String getContenido_url() {
        return contenido_url;
    }

    public void setContenido_url(String contenido_url) {
        this.contenido_url = contenido_url;
    }

    public LocalDate getFecha_publicacion() {
        return fecha_publicacion;
    }

    public void setFecha_publicacion(LocalDate fecha_publicacion) {
        this.fecha_publicacion = fecha_publicacion;
    }

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }
}
