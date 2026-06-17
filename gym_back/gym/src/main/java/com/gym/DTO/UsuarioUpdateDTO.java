package com.gym.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public class UsuarioUpdateDTO {

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "Solo letras y espacios")
    private String nombre;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "Solo letras y espacios")
    private String apellido;

    @Email(message = "Email no válido")
    private String email;

    @Pattern(regexp = "^9[0-9]{8}$", message = "Debe empezar con 9 y tener 9 dígitos")
    private String telefono;

    private String direccion;
    private String fecha_nacimiento;
    private String currentPassword;

    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9]).{8,}$", message = "Mínimo 8 caracteres, 1 mayúscula y 1 número")
    private String newPassword;
    private String estado;
    private String rol;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getFecha_nacimiento() { return fecha_nacimiento; }
    public void setFecha_nacimiento(String fecha_nacimiento) { this.fecha_nacimiento = fecha_nacimiento; }

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
