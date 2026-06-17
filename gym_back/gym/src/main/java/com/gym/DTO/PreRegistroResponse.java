package com.gym.DTO;

public class PreRegistroResponse {
    private String tempToken;
    private String mensaje;
    private Long dni;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String estado;

    public PreRegistroResponse() {}

    public PreRegistroResponse(String tempToken, String mensaje, Long dni, String nombre,
                               String apellido, String email, String telefono, String estado) {
        this.tempToken = tempToken;
        this.mensaje = mensaje;
        this.dni = dni;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
        this.estado = estado;
    }

    public String getTempToken() { return tempToken; }
    public void setTempToken(String tempToken) { this.tempToken = tempToken; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public Long getDni() { return dni; }
    public void setDni(Long dni) { this.dni = dni; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
