package com.alejo.parchaface.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "estadistica")
public class Estadistica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_estadistica;

    @OneToOne
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @Column(nullable = false)
    private Integer total_inscritos = 0;

    @Column(nullable = false)
    private Integer total_cancelaciones = 0;

    @Column(nullable = false)
    private LocalDate fecha_corte = LocalDate.now();

    // Getters y setters
    public Integer getId_estadistica() { return id_estadistica; }
    public void setId_estadistica(Integer id_estadistica) { this.id_estadistica = id_estadistica; }

    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }

    public Integer getTotal_inscritos() { return total_inscritos; }
    public void setTotal_inscritos(Integer total_inscritos) { this.total_inscritos = total_inscritos; }

    public Integer getTotal_cancelaciones() { return total_cancelaciones; }
    public void setTotal_cancelaciones(Integer total_cancelaciones) { this.total_cancelaciones = total_cancelaciones; }

    public LocalDate getFecha_corte() { return fecha_corte; }
    public void setFecha_corte(LocalDate fecha_corte) { this.fecha_corte = fecha_corte; }
}
