package com.gym.models;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "membresia_usuario")
public class MembresiaUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "DNI")
    private Long dni;

    @Column(name = "MEMBRESIA_ID")
    private Long id_membresia;

    @Column(name = "FECHA_INICIO")
    private LocalDate fecha_inicio;

    @Column(name = "FECHA_FIN")
    private LocalDate fecha_fin;

    @Column(name = "ESTADO")
    private String estado;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDni() {
        return dni;
    }

    public void setDni(Long dni) {
        this.dni = dni;
    }

    public Long getId_membresia() {
        return id_membresia;
    }

    public void setId_membresia(Long id_membresia) {
        this.id_membresia = id_membresia;
    }

    public LocalDate getFecha_inicio() {
        return fecha_inicio;
    }

    public void setFecha_inicio(LocalDate fecha_inicio) {
        this.fecha_inicio = fecha_inicio;
    }

    public LocalDate getFecha_fin() {
        return fecha_fin;
    }

    public void setFecha_fin(LocalDate fecha_fin) {
        this.fecha_fin = fecha_fin;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}