package com.gym.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;



@Entity
@Table(name = "CLASES")
public class Clase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CLASE")
    private Long idClase;

    @Column(name = "NOMBRE")
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombre;

    @Column(name = "FECHA_CLASE")
    @NotNull(message = "La fecha de la clase es obligatoria")
    private java.sql.Date fechaClase;

    @Column(name = "HORARIO")
    private String horario;

    @Column(name = "HORAI")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Formato HH:mm requerido (ej: 08:30)")
    private String horai;

    @Column(name = "HORAF")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Formato HH:mm requerido (ej: 08:30)")
    private String horaf;

    @Column(name = "ID_TRAINER")
    @NotNull(message = "El entrenador es obligatorio")
    private Long idTrainer;

    @Column(name = "ESTADO")
    private String estado;

    @Column(name = "CUPO")
    @NotNull(message = "El cupo es obligatorio")
    @Min(value = 10, message = "Cupo mínimo 10")
    @Max(value = 30, message = "Cupo máximo 30")
    private Integer cupo;

    public Long getIdClase() {
        return idClase;
    }

    public void setIdClase(Long idClase) {
        this.idClase = idClase;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public java.sql.Date getFechaClase() {
        return fechaClase;
    }

    public void setFechaClase(java.sql.Date fechaClase) {
        this.fechaClase = fechaClase;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public String getHorai() {
        return horai;
    }

    public void setHorai(String horai) {
        this.horai = horai;
    }

    public String getHoraf() {
        return horaf;
    }

    public void setHoraf(String horaf) {
        this.horaf = horaf;
    }

    public Long getIdTrainer() {
        return idTrainer;
    }

    public void setIdTrainer(Long idTrainer) {
        this.idTrainer = idTrainer;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getCupo() {
        return cupo;
    }

    public void setCupo(Integer cupo) {
        this.cupo = cupo;
    }
}
