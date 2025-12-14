package com.alejo.parchaface.model;

import jakarta.persistence.*;

@Entity
@Table(name = "archivo")
public class Archivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_archivo;

    @ManyToOne
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;  // Relación con Evento

    @Column(length = 150, nullable = false)
    private String nombre_archivo;

    @Column(length = 255, nullable = false)
    private String ruta_archivo;

    @Column(length = 50)
    private String tipo_archivo;

    // Getters y setters
    public Integer getId_archivo() { return id_archivo; }
    public void setId_archivo(Integer id_archivo) { this.id_archivo = id_archivo; }

    public Evento getEvento() {return evento;}
    public void setEvento(Evento evento) {this.evento = evento;}

    public String getNombre_archivo() { return nombre_archivo; }
    public void setNombre_archivo(String nombre_archivo) { this.nombre_archivo = nombre_archivo; }

    public String getRuta_archivo() { return ruta_archivo; }
    public void setRuta_archivo(String ruta_archivo) { this.ruta_archivo = ruta_archivo; }

    public String getTipo_archivo() { return tipo_archivo; }
    public void setTipo_archivo(String tipo_archivo) { this.tipo_archivo = tipo_archivo; }
}
