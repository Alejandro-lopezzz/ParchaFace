package com.alejo.parchaface.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reporte")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idReporte;

    @Column(length = 50)
    private String tipo;

    @Column(nullable = false)
    private LocalDateTime fechaGeneracion = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "id_admin")
    private Usuario admin;

    // Getters y setters
    public Integer getIdReporte() { return idReporte; }
    public void setIdReporte(Integer idReporte) { this.idReporte = idReporte; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }

    public Usuario getAdmin() { return admin; }
    public void setAdmin(Usuario admin) { this.admin = admin; }
}
