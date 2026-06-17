package com.gym.controllers;

import com.gym.models.Clase;
import com.gym.security.JwtUtil;
import com.gym.services.ClaseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clases")
public class ClaseController {

    private final ClaseService claseService;

    @Autowired
    private JwtUtil jwtUtil;

    public ClaseController(ClaseService claseService) {
        this.claseService = claseService;
    }

    private String getRole(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtUtil.extractRole(authHeader.substring(7));
        }
        return null;
    }

    private boolean isPersonalOrAdmin(String role) {
        return "PERSONAL".equals(role) || "ADMIN".equals(role);
    }

    @GetMapping
    public List<Clase> getAll() {
        return claseService.getAllClases();
    }

    @GetMapping("/trainer/{idTrainer}")
    public List<Clase> getByTrainer(@PathVariable Long idTrainer) {
        return claseService.getClasesByTrainer(idTrainer);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Clase> getById(@PathVariable Long id) {
        Clase clase = claseService.getClaseById(id);
        if (clase == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(clase);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Clase clase, HttpServletRequest request) {
        String role = getRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado"));
        }
        if (!isPersonalOrAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo PERSONAL o ADMIN pueden crear clases"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(claseService.createClase(clase));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Clase data, HttpServletRequest request) {
        String role = getRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado"));
        }
        if (!isPersonalOrAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo PERSONAL o ADMIN pueden modificar clases"));
        }
        Clase updated = claseService.updateClase(id, data);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Clase no encontrada"));
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        String role = getRole(request);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado"));
        }
        if (!isPersonalOrAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo PERSONAL o ADMIN pueden eliminar clases"));
        }
        boolean ok = claseService.deleteClase(id);
        return ok
                ? ResponseEntity.ok(Map.of("mensaje", "Clase eliminada"))
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No se encontró la clase"));
    }
}
