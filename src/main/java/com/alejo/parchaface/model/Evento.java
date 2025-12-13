package com.alejo.parchaface.model;

import com.alejo.parchaface.model.enums.EstadoEvento;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "evento")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_evento;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEvento estadoEvento;  // Usando enum

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inscripcion> inscripciones;

    // Getters y setters
    public Integer getId_evento() { return id_evento; }
    public void setId_evento(Integer id_evento) { this.id_evento = id_evento; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public EstadoEvento getEstadoEvento() { return estadoEvento; }
    public void setEstadoEvento(EstadoEvento estadoEvento) { this.estadoEvento = estadoEvento; }

    public List<Inscripcion> getInscripciones() { return inscripciones; }
    public void setInscripciones(List<Inscripcion> inscripciones) { this.inscripciones = inscripciones; }
}
