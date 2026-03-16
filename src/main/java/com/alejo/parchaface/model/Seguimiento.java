package com.alejo.parchaface.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "seguimiento",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_seguimiento", columnNames = {"id_seguidor", "id_seguido"})
        }
)
public class Seguimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seguimiento")
    private Integer idSeguimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_seguidor", nullable = false)
    private Usuario seguidor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_seguido", nullable = false)
    private Usuario seguido;

    @Column(name = "fecha_seguimiento", nullable = false)
    private LocalDateTime fechaSeguimiento = LocalDateTime.now();

    public Integer getIdSeguimiento() {
        return idSeguimiento;
    }

    public void setIdSeguimiento(Integer idSeguimiento) {
        this.idSeguimiento = idSeguimiento;
    }

    public Usuario getSeguidor() {
        return seguidor;
    }

    public void setSeguidor(Usuario seguidor) {
        this.seguidor = seguidor;
    }

    public Usuario getSeguido() {
        return seguido;
    }

    public void setSeguido(Usuario seguido) {
        this.seguido = seguido;
    }

    public LocalDateTime getFechaSeguimiento() {
        return fechaSeguimiento;
    }

    public void setFechaSeguimiento(LocalDateTime fechaSeguimiento) {
        this.fechaSeguimiento = fechaSeguimiento;
    }
}