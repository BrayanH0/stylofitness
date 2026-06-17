package com.gym.models;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "USUARIO")
@JsonIgnoreProperties(value = {"passwordHash", "password"}, allowSetters = true)
public class Usuario {
    @Id
    @NotNull(message = "El DNI es obligatorio")
    @Min(value = 10000000, message = "El DNI debe tener 8 dígitos numéricos")
    @Max(value = 99999999, message = "El DNI debe tener 8 dígitos numéricos")
    private Long dni;

    @Column(name = "NOMBRE")
    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "Solo letras y espacios")
    @Size(max = 50, message = "Máximo 50 caracteres")
    private String nombre;

    @Column(name = "APELLIDO")
    @NotBlank(message = "El apellido es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "Solo letras y espacios")
    @Size(max = 50, message = "Máximo 50 caracteres")
    private String apellido;

    @Column(name = "TELEFONO")
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^9[0-9]{8}$", message = "Debe empezar con 9 y tener 9 dígitos")
    private String telefono;

    @Column(name = "EMAIL")
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email no válido")
    private String email;

    @Column(name = "FECHA_NACIMIENTO")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Formato YYYY-MM-DD requerido")
    private String fecha_nacimiento;

    @Column(name = "DIRECCION")
    @NotBlank(message = "La dirección es obligatoria")
    @Size(min = 5, max = 200, message = "Entre 5 y 200 caracteres")
    private String direccion;

    @Column(name = "FECHA_REGISTRO")
    private LocalDate fecha_registro;

    @Column(name = "ESTADO")
    private String estado;

    @Column(name = "PASSWORD")
    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9]).{8,}$", message = "Mínimo 8 caracteres, 1 mayúscula y 1 número")
    private String passwordHash;

    @Column(name = "ROL")
    private String rol;

    @Column(name = "FECHA_CONTRATACION")
    private LocalDate fecha_contratacion;

    public Long getDni() {
        return dni;
    }
    public void setDni(Long dni) {
        this.dni = dni;
    }
    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public String getApellido() {
        return apellido;
    }
    public void setApellido(String apellido) {
        this.apellido = apellido;
    }
    public String getTelefono() {
        return telefono;
    }
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getFecha_nacimiento() {
        return fecha_nacimiento;
    }
    public void setFecha_nacimiento(String fecha_nacimiento) {
        this.fecha_nacimiento = fecha_nacimiento;
    }
    public String getDireccion() {
        return direccion;
    }
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
    public LocalDate getFecha_registro() {
        return fecha_registro;
    }
    public void setFecha_registro(LocalDate fecha_registro) {
        this.fecha_registro = fecha_registro;
    }
    public String getEstado() {
        return estado;
    }
    public void setEstado(String estado) {
        this.estado = estado;
    }
    public String getPasswordHash() {
        return passwordHash;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    public String getPassword() {
        return passwordHash;
    }
    public void setPassword(String password) {
        this.passwordHash = password;
    }
    public String getRol() {
        return rol;
    }
    public void setRol(String rol) {
        this.rol = rol;
    }
    public LocalDate getFecha_contratacion() {
        return fecha_contratacion;
    }
    public void setFecha_contratacion(LocalDate fecha_contratacion) {
        this.fecha_contratacion = fecha_contratacion;
    }
}