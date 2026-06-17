package com.gym.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "INSCRIPCION_CLASE")
public class InscripcionClase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_INSCRIPCION")
    private Long idInscripcion;

    @Column(name = "DNI_USUARIO")
    private Long dniUsuario;

    @Column(name = "ID_CLASE")
    private Long idClase;

    @Column(name = "FECHA_INSCRIPCION")
    private LocalDateTime fechaInscripcion;

    @Column(name = "ESTADO")
    private String estado;

    public Long getIdInscripcion() {
        return idInscripcion;
    }

    public void setIdInscripcion(Long idInscripcion) {
        this.idInscripcion = idInscripcion;
    }

    public Long getDniUsuario() {
        return dniUsuario;
    }

    public void setDniUsuario(Long dniUsuario) {
        this.dniUsuario = dniUsuario;
    }

    public Long getIdClase() {
        return idClase;
    }

    public void setIdClase(Long idClase) {
        this.idClase = idClase;
    }

    public LocalDateTime getFechaInscripcion() {
        return fechaInscripcion;
    }

    public void setFechaInscripcion(LocalDateTime fechaInscripcion) {
        this.fechaInscripcion = fechaInscripcion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}