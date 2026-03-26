package com.alejo.parchaface.model;

import com.alejo.parchaface.model.enums.PlataformaSocial;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(
        name = "evento_red_social",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_evento_plataforma", columnNames = {"id_evento", "plataforma"})
        }
)
public class EventoRedSocial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento_red_social")
    private Integer idEventoRedSocial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    @JsonIgnore
    private Evento evento;

    @Enumerated(EnumType.STRING)
    @Column(name = "plataforma", nullable = false, length = 30)
    private PlataformaSocial plataforma;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;

    public Integer getIdEventoRedSocial() { return idEventoRedSocial; }
    public void setIdEventoRedSocial(Integer idEventoRedSocial) { this.idEventoRedSocial = idEventoRedSocial; }

    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }

    public PlataformaSocial getPlataforma() { return plataforma; }
    public void setPlataforma(PlataformaSocial plataforma) { this.plataforma = plataforma; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
}