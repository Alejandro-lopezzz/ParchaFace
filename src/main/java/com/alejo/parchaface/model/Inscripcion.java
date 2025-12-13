package com.alejo.parchaface.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "inscripcion")
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_inscripcion;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;  // Cambié de id_usuario a usuario

    @ManyToOne
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;    // Cambié de id_evento a evento

    @Column(nullable = false)
    private LocalDate fecha_inscripcion = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoInscripcion estado_inscripcion = EstadoInscripcion.VIGENTE;

    // Getters y setters
    public Integer getId_inscripcion() { return id_inscripcion; }
    public void setId_inscripcion(Integer id_inscripcion) { this.id_inscripcion = id_inscripcion; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }

    public LocalDate getFecha_inscripcion() { return fecha_inscripcion; }
    public void setFecha_inscripcion(LocalDate fecha_inscripcion) { this.fecha_inscripcion = fecha_inscripcion; }

    public EstadoInscripcion getEstado_inscripcion() { return estado_inscripcion; }
    public void setEstado_inscripcion(EstadoInscripcion estado_inscripcion) { this.estado_inscripcion = estado_inscripcion; }

    public enum EstadoInscripcion { VIGENTE, CANCELADA }
}
