package com.alejo.parchaface.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reporte")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_reporte;

    @Column(length = 50)
    private String tipo;

    @Column(nullable = false)
    private LocalDateTime fecha_generacion = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "id_admin")
    private Usuario id_admin;

    // Getters y setters
    public Integer getId_reporte() { return id_reporte; }
    public void setId_reporte(Integer id_reporte) { this.id_reporte = id_reporte; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public LocalDateTime getFecha_generacion() { return fecha_generacion; }
    public void setFecha_generacion(LocalDateTime fecha_generacion) { this.fecha_generacion = fecha_generacion; }
    public Usuario getId_admin() { return id_admin; }
    public void setId_admin(Usuario id_admin) { this.id_admin = id_admin; }
}
