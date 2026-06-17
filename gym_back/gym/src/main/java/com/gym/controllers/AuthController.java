package com.gym.controllers;

import com.gym.DTO.LoginRequest;
import com.gym.DTO.LoginResponseDTO;
import com.gym.models.Usuario;
import com.gym.services.UsuarioService;
import com.gym.security.JwtUtil;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioService usuarioServices;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Long dniLong = Long.parseLong(request.getDni());
            Usuario u = usuarioServices.login(dniLong, request.getPassword());

            String rol = u.getRol() != null ? u.getRol() : "CLIENTE";

            String token = jwtUtil.generateToken(u.getDni(), u.getNombre(), rol);

            LoginResponseDTO response = new LoginResponseDTO(
                token,
                u.getDni(),
                u.getNombre(),
                u.getApellido(),
                u.getEmail(),
                u.getTelefono(),
                u.getEstado(),
                rol
            );

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al procesar el login: " + e.getMessage()));
        }
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "Token no proporcionado"));
            }

            String token = authHeader.substring(7);

            if (jwtUtil.validateToken(token)) {
                Long dni = jwtUtil.extractDni(token);
                String nombre = jwtUtil.extractNombre(token);
                String rol = jwtUtil.extractRole(token);

                return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "dni", dni,
                    "nombre", nombre,
                    "rol", rol
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "Token inválido o expirado"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("valid", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/post-pago-login")
    public ResponseEntity<?> postPagoLogin(@RequestBody Map<String, Object> body) {
        try {
            Long dni = Long.parseLong(body.get("dni").toString());
            String tempToken = (String) body.get("tempToken");

            if (!jwtUtil.validateTempToken(tempToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token temporal invalido o expirado"));
            }

            Long tokenDni = jwtUtil.extractDni(tempToken);
            if (!tokenDni.equals(dni)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token no corresponde al usuario"));
            }

            Usuario u = usuarioServices.obtenerPorDni(dni);
            if (u == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado"));
            }

            if (!"ACTIVO".equalsIgnoreCase(u.getEstado())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Cuenta no activada. Completa el pago para activar tu cuenta."));
            }

            String rol = u.getRol() != null ? u.getRol() : "CLIENTE";
            String token = jwtUtil.generateToken(u.getDni(), u.getNombre(), rol);

            LoginResponseDTO response = new LoginResponseDTO(
                token, u.getDni(), u.getNombre(), u.getApellido(),
                u.getEmail(), u.getTelefono(), u.getEstado(), rol
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error en post-pago login: " + e.getMessage()));
        }
    }
}