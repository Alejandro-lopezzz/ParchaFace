package com.alejo.parchaface.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_code")
public class PasswordResetCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String correo;

    @Column(nullable=false, length=200)
    private String codigoHash;

    @Column(nullable=false)
    private LocalDateTime creadoEn;

    @Column(nullable=false)
    private LocalDateTime expiraEn;

    @Column(nullable=false)
    private boolean usado;

    @PrePersist
    public void prePersist() {
        creadoEn = LocalDateTime.now();
        if (expiraEn == null) expiraEn = creadoEn.plusMinutes(10);
        usado = false;
    }

    // getters/setters
    public Long getId() { return id; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getCodigoHash() { return codigoHash; }
    public void setCodigoHash(String codigoHash) { this.codigoHash = codigoHash; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getExpiraEn() { return expiraEn; }
    public void setExpiraEn(LocalDateTime expiraEn) { this.expiraEn = expiraEn; }

    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
}
