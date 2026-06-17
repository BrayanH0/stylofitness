package com.gym.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gym.DTO.PreRegistroResponse;
import com.gym.DTO.UsuarioUpdateDTO;
import com.gym.models.Usuario;
import com.gym.services.UsuarioService;
import com.gym.security.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuario")
public class UsuarioController {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);

    @Autowired
    private UsuarioService usuarioServices;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Map<String, Object> usuarioToPublicMap(Usuario u) {
        Map<String, Object> m = new HashMap<>();
        m.put("dni", u.getDni());
        m.put("nombre", u.getNombre());
        m.put("apellido", u.getApellido());
        m.put("email", u.getEmail());
        m.put("telefono", u.getTelefono());
        m.put("estado", u.getEstado());
        m.put("rol", u.getRol());
        m.put("fecha_registro", u.getFecha_registro());
        return m;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> mostrar() {
        List<Usuario> usuarios = this.usuarioServices.mostrar_usuario();
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Usuario u : usuarios) {
            lista.add(usuarioToPublicMap(u));
        }
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/existe-dni/{dni}")
    public ResponseEntity<?> existeDni(@PathVariable Long dni) {
        boolean existe = usuarioServices.existeDni(dni);
        return ResponseEntity.ok(Map.of("existe", existe));
    }

    @PostMapping("/pre-registro")
    public ResponseEntity<?> preRegistro(@Valid @RequestBody Usuario usu) {
        try {
            usu.setEstado("INACTIVO");
            Usuario saved = usuarioServices.crear_usuario(usu);

            String tempToken = jwtUtil.generateTempToken(saved.getDni(), 15 * 60 * 1000);

            PreRegistroResponse response = new PreRegistroResponse(
                tempToken, "Pre-registro exitoso",
                saved.getDni(), saved.getNombre(), saved.getApellido(),
                saved.getEmail(), saved.getTelefono(), saved.getEstado()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("Error en pre-registro", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/activar/{dni}")
    public ResponseEntity<?> activarUsuario(@PathVariable Long dni) {
        try {
            Usuario usuario = usuarioServices.obtenerPorDni(dni);

            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado"));
            }

            usuario.setEstado("ACTIVO");
            usuarioServices.actualizar(usuario);

            return ResponseEntity.ok(Map.of("mensaje", "Usuario activado correctamente"));

        } catch (Exception ex) {
            logger.error("Error al activar usuario", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/perfil/{dni}")
    public ResponseEntity<?> actualizarPerfil(@PathVariable Long dni, @Valid @RequestBody UsuarioUpdateDTO datos,
                                             HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No autenticado"));
            }

            String token = authHeader.substring(7);
            Long tokenDni = jwtUtil.extractDni(token);

            if (!tokenDni.equals(dni)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Solo puede editar su propio perfil"));
            }

            Usuario usuario = usuarioServices.obtenerPorDni(dni);
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado"));
            }

            if (datos.getNombre() != null) usuario.setNombre(datos.getNombre());
            if (datos.getApellido() != null) usuario.setApellido(datos.getApellido());
            if (datos.getEmail() != null) usuario.setEmail(datos.getEmail());
            if (datos.getTelefono() != null) usuario.setTelefono(datos.getTelefono());
            if (datos.getDireccion() != null) usuario.setDireccion(datos.getDireccion());
            if (datos.getFecha_nacimiento() != null) usuario.setFecha_nacimiento(datos.getFecha_nacimiento());

            if (datos.getNewPassword() != null && !datos.getNewPassword().isEmpty()) {
                if (datos.getCurrentPassword() == null || datos.getCurrentPassword().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Debe proporcionar la contraseña actual para cambiarla"));
                }
                if (!passwordEncoder.matches(datos.getCurrentPassword(), usuario.getPasswordHash())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Contraseña actual incorrecta"));
                }
                usuario.setPasswordHash(passwordEncoder.encode(datos.getNewPassword()));
            }

            Usuario saved = usuarioServices.actualizar(usuario);
            return ResponseEntity.ok(saved);

        } catch (Exception ex) {
            logger.error("Error al actualizar perfil", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/existe-email")
    public ResponseEntity<?> existeEmail(@RequestParam String email) {
        boolean existe = usuarioServices.existeEmail(email);
        return ResponseEntity.ok(Map.of("existe", existe));
    }

    @GetMapping("/existe-telefono")
    public ResponseEntity<?> existeTelefono(@RequestParam String telefono) {
        boolean existe = usuarioServices.existeTelefono(telefono);
        return ResponseEntity.ok(Map.of("existe", existe));
    }

    @PostMapping("/retomar-pago")
    public ResponseEntity<?> retomarPago(@RequestBody Usuario datosNuevos) {
        try {
            Long dni = datosNuevos.getDni();
            if (dni == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "DNI requerido"));
            }
            Usuario usuario = usuarioServices.obtenerPorDni(dni);
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado"));
            }
            if (!"INACTIVO".equals(usuario.getEstado())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El usuario ya está activo. Inicia sesión."));
            }

            // Validar unicidad solo si el campo cambió respecto al existente
            if (datosNuevos.getEmail() != null && !datosNuevos.getEmail().equals(usuario.getEmail())) {
                if (usuarioServices.existeEmail(datosNuevos.getEmail())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "El email ya está registrado."));
                }
                usuario.setEmail(datosNuevos.getEmail());
            }
            if (datosNuevos.getTelefono() != null && !datosNuevos.getTelefono().equals(usuario.getTelefono())) {
                if (usuarioServices.existeTelefono(datosNuevos.getTelefono())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "El teléfono ya está registrado."));
                }
                usuario.setTelefono(datosNuevos.getTelefono());
            }

            if (datosNuevos.getNombre() != null) usuario.setNombre(datosNuevos.getNombre());
            if (datosNuevos.getApellido() != null) usuario.setApellido(datosNuevos.getApellido());
            if (datosNuevos.getDireccion() != null) usuario.setDireccion(datosNuevos.getDireccion());
            if (datosNuevos.getFecha_nacimiento() != null) usuario.setFecha_nacimiento(datosNuevos.getFecha_nacimiento());

            // Re-hashear contraseña solo si se envió una nueva (llega como plaintext en passwordHash)
            if (datosNuevos.getPasswordHash() != null && !datosNuevos.getPasswordHash().isEmpty()) {
                usuario.setPasswordHash(passwordEncoder.encode(datosNuevos.getPasswordHash()));
            }

            usuarioServices.actualizar(usuario);

            String tempToken = jwtUtil.generateTempToken(dni, 15 * 60 * 1000);
            return ResponseEntity.ok(Map.of(
                "tempToken", tempToken,
                "dni", usuario.getDni(),
                "nombre", usuario.getNombre(),
                "mensaje", "Datos actualizados y token renovado para retomar el pago"
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("Error en retomar-pago", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }
}