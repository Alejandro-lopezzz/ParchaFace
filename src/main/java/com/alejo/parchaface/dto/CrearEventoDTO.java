package com.alejo.parchaface.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

public class CrearEventoDTO {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150)
    private String titulo;

    @Size(max = 500)
    private String descripcion;

    @NotNull(message = "La fecha es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    @NotNull(message = "La hora es obligatoria")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime hora;

    @NotBlank(message = "La ubicación es obligatoria")
    @Size(max = 200)
    private String ubicacion;


    @NotNull(message = "El cupo es obligatorio")
    @Min(value = 1, message = "El cupo debe ser mayor a 0")
    private Integer cupo;

    @Size(max = 80)
    private String categoria;

    // getters / setters
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public Integer getCupo() { return cupo; }
    public void setCupo(Integer cupo) { this.cupo = cupo; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
}
