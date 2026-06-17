package com.gym.controllers;

import com.gym.models.InscripcionClase;
import com.gym.models.Usuario;
import com.gym.repository.UsuarioRepository;
import com.gym.services.InscripcionClaseService;
import com.gym.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inscripciones")
public class InscripcionController {

    @Autowired
    private InscripcionClaseService inscripcionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getAuthenticatedDni(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.extractDni(token);
        }
        return null;
    }

    private String getAuthenticatedRole(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.extractRole(token);
        }
        return null;
    }

    private boolean isAdminOrPersonal(String role) {
        return "ADMIN".equals(role) || "PERSONAL".equals(role);
    }

    @GetMapping("/usuario/{dni}")
    public ResponseEntity<?> getInscripcionesByUsuario(@PathVariable Long dni, HttpServletRequest request) {
        Long authenticatedDni = getAuthenticatedDni(request);
        String role = getAuthenticatedRole(request);

        if (authenticatedDni == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
        }

        if (!authenticatedDni.equals(dni) && !isAdminOrPersonal(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo puede ver sus propias inscripciones"));
        }

        List<InscripcionClase> inscripciones = inscripcionService.getInscripcionesByUsuario(dni);
        return ResponseEntity.ok(inscripciones);
    }

    @GetMapping("/usuario/{dni}/activas")
    public ResponseEntity<?> getInscripcionesActivasByUsuario(@PathVariable Long dni, HttpServletRequest request) {
        Long authenticatedDni = getAuthenticatedDni(request);
        String role = getAuthenticatedRole(request);

        if (authenticatedDni == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
        }

        if (!authenticatedDni.equals(dni) && !isAdminOrPersonal(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo puede ver sus propias inscripciones"));
        }

        List<InscripcionClase> inscripciones = inscripcionService.getInscripcionesActivasByUsuario(dni);
        return ResponseEntity.ok(inscripciones);
    }

    @GetMapping("/clase/{idClase}")
    public ResponseEntity<?> getInscripcionesByClase(@PathVariable Long idClase, HttpServletRequest request) {
        Long authenticatedDni = getAuthenticatedDni(request);
        String role = getAuthenticatedRole(request);

        if (authenticatedDni == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
        }
        if (!isAdminOrPersonal(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo ADMIN o PERSONAL pueden ver los inscritos de una clase"));
        }
        List<InscripcionClase> inscripciones = inscripcionService.getInscripcionesByClase(idClase);
        List<Map<String, Object>> resultado = inscripciones.stream().map(ins -> {
            Map<String, Object> item = new HashMap<>();
            item.put("idInscripcion", ins.getIdInscripcion());
            item.put("dniUsuario", ins.getDniUsuario());
            item.put("fechaInscripcion", ins.getFechaInscripcion());
            item.put("estado", ins.getEstado());
            Usuario u = usuarioRepository.findByDni(ins.getDniUsuario());
            if (u != null) {
                item.put("nombreUsuario", u.getNombre() + " " + u.getApellido());
            } else {
                item.put("nombreUsuario", "DNI: " + ins.getDniUsuario());
            }
            return item;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/clase/{idClase}/cupos")
    public ResponseEntity<Map<String, Integer>> getCuposDisponibles(@PathVariable Long idClase) {
        int disponibles = inscripcionService.getCuposDisponibles(idClase);
        return ResponseEntity.ok(Map.of("cuposDisponibles", disponibles));
    }

    @GetMapping("/verificar/{dni}/{idClase}")
    public ResponseEntity<?> verificarInscripcion(
            @PathVariable Long dni,
            @PathVariable Long idClase,
            HttpServletRequest request) {
        Long authenticatedDni = getAuthenticatedDni(request);
        String role = getAuthenticatedRole(request);

        if (authenticatedDni == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
        }

        if (!authenticatedDni.equals(dni) && !isAdminOrPersonal(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo puede verificar sus propias inscripciones"));
        }

        boolean inscrito = inscripcionService.isInscrito(dni, idClase);
        return ResponseEntity.ok(Map.of("inscrito", inscrito));
    }

    @PostMapping("/inscribir")
    public ResponseEntity<?> inscribirUsuario(
            @RequestParam Long dniUsuario,
            @RequestParam Long idClase,
            HttpServletRequest request) {
        Long authenticatedDni = getAuthenticatedDni(request);
        String role = getAuthenticatedRole(request);

        if (authenticatedDni == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
        }

        if (!authenticatedDni.equals(dniUsuario) && !isAdminOrPersonal(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo puede inscribirse a si mismo"));
        }

        try {
            InscripcionClase inscripcion = inscripcionService.inscribirUsuario(dniUsuario, idClase);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                        "mensaje", "Inscripción exitosa",
                        "idInscripcion", inscripcion.getIdInscripcion()
                    ));
        } catch (com.gym.exceptions.ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (com.gym.exceptions.BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error inesperado: " + e.getMessage()));
        }
    }

    @PutMapping("/cancelar")
    public ResponseEntity<?> cancelarInscripcion(
            @RequestParam Long dniUsuario,
            @RequestParam Long idClase,
            HttpServletRequest request) {
        Long authenticatedDni = getAuthenticatedDni(request);
        String role = getAuthenticatedRole(request);

        if (authenticatedDni == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
        }

        if (!authenticatedDni.equals(dniUsuario) && !isAdminOrPersonal(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo puede cancelar sus propias inscripciones"));
        }

        try {
            inscripcionService.cancelarInscripcion(dniUsuario, idClase);
            return ResponseEntity.ok(Map.of("mensaje", "Inscripción cancelada correctamente"));
        } catch (com.gym.exceptions.ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (com.gym.exceptions.BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error inesperado: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{idInscripcion}")
    public ResponseEntity<?> eliminarInscripcion(@PathVariable Long idInscripcion, HttpServletRequest request) {
        Long authenticatedDni = getAuthenticatedDni(request);
        String role = getAuthenticatedRole(request);

        if (authenticatedDni == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
        }

        if (!isAdminOrPersonal(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo ADMIN o PERSONAL pueden eliminar inscripciones"));
        }

        try {
            inscripcionService.eliminarInscripcion(idInscripcion);
            return ResponseEntity.ok(Map.of("mensaje", "Inscripción eliminada"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}