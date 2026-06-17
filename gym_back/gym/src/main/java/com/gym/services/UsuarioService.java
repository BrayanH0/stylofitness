package com.gym.services;

import java.util.ArrayList;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

import com.gym.models.Usuario;
import com.gym.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    public ArrayList<Usuario> mostrar_usuario() {
        return (ArrayList<Usuario>) usuarioRepository.findAll();
    }

    public synchronized Usuario crear_usuario(Usuario e) {
        if (usuarioRepository.existsByDni(e.getDni())) {
            throw new IllegalArgumentException("El DNI ya está registrado.");
        }
        validarEmailTelefonoUnicos(e);
        if (e.getPasswordHash() == null || e.getPasswordHash().isEmpty()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        validarEdad(e.getFecha_nacimiento());
        String hashed = passwordEncoder.encode(e.getPasswordHash());
        e.setPasswordHash(hashed);
        if (e.getRol() == null || e.getRol().isEmpty()) {
            e.setRol("CLIENTE");
        }
        if (e.getEstado() == null || e.getEstado().isEmpty()) {
            e.setEstado("INACTIVO");
        }
        try {
            return usuarioRepository.save(e);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("El DNI ya está registrado.");
        }
    }

    public synchronized Usuario crear_personal(Usuario e) {
        if (usuarioRepository.existsByDni(e.getDni())) {
            throw new IllegalArgumentException("El DNI ya está registrado.");
        }
        validarEmailTelefonoUnicos(e);
        if (e.getPasswordHash() == null || e.getPasswordHash().isEmpty()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        validarEdad(e.getFecha_nacimiento());
        String hashed = passwordEncoder.encode(e.getPasswordHash());
        e.setPasswordHash(hashed);
        e.setRol("PERSONAL");
        if (e.getEstado() == null || e.getEstado().isEmpty()) {
            e.setEstado("ACTIVO");
        }
        if (e.getFecha_contratacion() == null) {
            e.setFecha_contratacion(LocalDate.now());
        }
        try {
            return usuarioRepository.save(e);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("El DNI ya está registrado.");
        }
    }

    private void validarEdad(String fechaNacimiento) {
        if (fechaNacimiento == null || fechaNacimiento.isBlank()) {
            return;
        }
        try {
            LocalDate fechaNac = LocalDate.parse(fechaNacimiento);
            LocalDate hoy = LocalDate.now();
            int edad = hoy.getYear() - fechaNac.getYear();
            if (hoy.getDayOfYear() < fechaNac.getDayOfYear()) {
                edad--;
            }
            if (edad < 10) {
                throw new IllegalArgumentException("Debes tener al menos 10 años");
            }
            if (edad > 100) {
                throw new IllegalArgumentException("Edad máxima permitida: 100 años");
            }
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException("Fecha de nacimiento no válida");
        }
    }

    private void validarEmailTelefonoUnicos(Usuario e) {
        if (usuarioRepository.existsByEmail(e.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado.");
        }
        if (usuarioRepository.existsByTelefono(e.getTelefono())) {
            throw new IllegalArgumentException("El teléfono ya está registrado.");
        }
    }

    public Boolean eliminar_usuario(Long dni) {
        usuarioRepository.deleteById(dni);
        return true;
    }

    public Optional<Usuario> getUsuario(Long dni) {
        return usuarioRepository.findById(dni);
    }

    public Usuario login(Long dni, String rawPassword) {
        Usuario u = usuarioRepository.findByDni(dni);
        if (u == null) {
            throw new RuntimeException("Usuario no encontrado");
        }

        if (!passwordEncoder.matches(rawPassword, u.getPasswordHash())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        if (!"ACTIVO".equalsIgnoreCase(u.getEstado())) {
            throw new RuntimeException("Usuario inactivo");
        }

        return u;
    }

    public boolean existeDni(Long dni) {
        return usuarioRepository.existsByDni(dni);
    }

    public Usuario obtenerPorDni(Long dni) {
        return usuarioRepository.findByDni(dni);
    }

    public ArrayList<Usuario> listarPersonal() {
        return (ArrayList<Usuario>) usuarioRepository.findByRol("PERSONAL");
    }

    public Usuario actualizar(Usuario u) {
        return usuarioRepository.save(u);
    }

    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    public boolean existeTelefono(String telefono) {
        return usuarioRepository.existsByTelefono(telefono);
    }

}
