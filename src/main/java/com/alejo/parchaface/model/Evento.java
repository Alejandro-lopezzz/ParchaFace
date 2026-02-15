    package com.alejo.parchaface.model;

    import java.time.LocalDateTime;
    import com.alejo.parchaface.model.enums.EstadoEvento;
    import jakarta.persistence.*;
    import java.util.ArrayList;
    import java.util.List;
    import java.time.LocalTime;


    @Entity
    @Table(name = "evento")
    public class Evento {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id_evento")
        private Integer idEvento;

        @Column(name = "titulo", nullable = false)
        private String titulo;
        @Column(name = "descripcion", length = 500)
        private String descripcion;

        @Column(name = "fecha", nullable = false)
        private java.time.LocalDateTime fecha;

        @Column(name = "hora", nullable = false)
        private LocalTime hora;

        @Column(name = "lugar", length = 200, nullable = false)
        private String ubicacion;

        @Column(name = "cupo")
        private Integer cupo;

        @Column(name = "categoria", length = 80)
        private String categoria;

        @Column(name = "fecha_creacion", nullable = false)
        private java.time.LocalDateTime fechaCreacion;


        @Enumerated(EnumType.STRING)
        @Column(name = "estado_evento", nullable = false)
        private EstadoEvento estadoEvento;  // Usando enum

        @ManyToOne
        @JoinColumn(name = "id_organizador", nullable = false)
        private Usuario organizador;

        @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<Inscripcion> inscripciones = new ArrayList<>();

        // Getters y setters
        public Integer getIdEvento() { return idEvento; }
        public void setIdEvento(Integer idEvento) { this.idEvento = idEvento; }

        public String getTitulo() { return titulo; }
        public void setTitulo(String titulo) { this.titulo = titulo; }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }

        public LocalDateTime getFecha() {
            return fecha;
        }

        public void setFecha(LocalDateTime fecha) {
            this.fecha = fecha;
        }

        public LocalTime getHora() {
            return hora;
        }

        public void setHora(LocalTime hora) {
            this.hora = hora;
        }


        public String getUbicacion() {
            return ubicacion;
        }

        public void setUbicacion(String ubicacion) {
            this.ubicacion = ubicacion;
        }

        public Integer getCupo() {
            return cupo;
        }

        public void setCupo(Integer cupo) {
            this.cupo = cupo;
        }

        public String getCategoria() {
            return categoria;
        }

        public void setCategoria(String categoria) {
            this.categoria = categoria;
        }

        public LocalDateTime getFechaCreacion() {
            return fechaCreacion;
        }

        public void setFechaCreacion(LocalDateTime fechaCreacion) {
            this.fechaCreacion = fechaCreacion;
        }


        public EstadoEvento getEstadoEvento() { return estadoEvento; }
        public void setEstadoEvento(EstadoEvento estadoEvento) { this.estadoEvento = estadoEvento; }

        public Usuario getOrganizador() { return organizador; }
        public void setOrganizador(Usuario organizador) { this.organizador = organizador; }

        public List<Inscripcion> getInscripciones() { return inscripciones; }
        public void setInscripciones(List<Inscripcion> inscripciones) { this.inscripciones = inscripciones; }

        @PrePersist
        public void prePersist() {
            if (this.fechaCreacion == null) {
                this.fechaCreacion = LocalDateTime.now();
            }
            if (this.estadoEvento == null) {
                this.estadoEvento = EstadoEvento.activo;
            }
            if (this.hora == null) {
                this.hora = LocalTime.of(0, 0); // o la hora que quieras por defecto
            }
        }


    }

