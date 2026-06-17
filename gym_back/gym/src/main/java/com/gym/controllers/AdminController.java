package com.gym.controllers;

import com.gym.models.Usuario;
import com.gym.models.Clase;
import com.gym.models.MembresiaUsuario;
import com.gym.services.UsuarioService;
import com.gym.exceptions.ResourceNotFoundException;
import com.gym.repository.PagoRepository;
import com.gym.repository.MembresiaUsuarioRepository;
import com.gym.repository.ClaseRepository;
import com.gym.repository.InscripcionClaseRepository;
import com.gym.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UsuarioService usuarioServices;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private MembresiaUsuarioRepository MembresiaUsuarioRepository;

    @Autowired
    private ClaseRepository claseRepository;

    @Autowired
    private InscripcionClaseRepository inscripcionClaseRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

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

    @GetMapping("/usuarios")
    public ResponseEntity<List<Map<String, Object>>> listarUsuarios() {
        List<Usuario> usuarios = usuarioServices.mostrar_usuario();
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Usuario u : usuarios) {
            lista.add(usuarioToPublicMap(u));
        }
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/usuarios/{dni}")
    public ResponseEntity<?> obtenerUsuario(@PathVariable Long dni) {
        Usuario usuario = usuarioServices.obtenerPorDni(dni);
        if (usuario == null) {
            throw new ResourceNotFoundException("Usuario", "DNI", dni);
        }
        return ResponseEntity.ok(usuario);
    }

    @PutMapping("/usuarios/{dni}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long dni, @Valid @RequestBody com.gym.DTO.UsuarioUpdateDTO usuarioActualizado) {
        Usuario usuarioExistente = usuarioServices.obtenerPorDni(dni);
        if (usuarioExistente == null) {
            throw new ResourceNotFoundException("Usuario", "DNI", dni);
        }

        if (usuarioActualizado.getNombre() != null) {
            usuarioExistente.setNombre(usuarioActualizado.getNombre());
        }
        if (usuarioActualizado.getApellido() != null) {
            usuarioExistente.setApellido(usuarioActualizado.getApellido());
        }
        if (usuarioActualizado.getEmail() != null) {
            usuarioExistente.setEmail(usuarioActualizado.getEmail());
        }
        if (usuarioActualizado.getTelefono() != null) {
            usuarioExistente.setTelefono(usuarioActualizado.getTelefono());
        }
        if (usuarioActualizado.getDireccion() != null) {
            usuarioExistente.setDireccion(usuarioActualizado.getDireccion());
        }
        if (usuarioActualizado.getFecha_nacimiento() != null) {
            usuarioExistente.setFecha_nacimiento(usuarioActualizado.getFecha_nacimiento());
        }
        if (usuarioActualizado.getEstado() != null) {
            Set<String> estadosPermitidos = Set.of("ACTIVO", "INACTIVO");
            if (!estadosPermitidos.contains(usuarioActualizado.getEstado().toUpperCase())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Estado no válido. Permitidos: ACTIVO, INACTIVO"));
            }
            usuarioExistente.setEstado(usuarioActualizado.getEstado());
        }
        if (usuarioActualizado.getRol() != null) {
            Set<String> rolesPermitidos = Set.of("ADMIN", "PERSONAL", "CLIENTE");
            if (!rolesPermitidos.contains(usuarioActualizado.getRol().toUpperCase())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Rol no válido. Permitidos: ADMIN, PERSONAL, CLIENTE"));
            }
            usuarioExistente.setRol(usuarioActualizado.getRol());
        }
        if (usuarioActualizado.getNewPassword() != null && !usuarioActualizado.getNewPassword().isEmpty()) {
            usuarioExistente.setPasswordHash(passwordEncoder.encode(usuarioActualizado.getNewPassword()));
        }

        Usuario usuarioGuardado = usuarioServices.actualizar(usuarioExistente);
        return ResponseEntity.ok(usuarioGuardado);
    }

    @DeleteMapping("/usuarios/{dni}")
    public ResponseEntity<?> desactivarUsuario(@PathVariable Long dni) {
        Usuario usuario = usuarioServices.obtenerPorDni(dni);
        if (usuario == null) {
            throw new ResourceNotFoundException("Usuario", "DNI", dni);
        }

        usuario.setEstado("INACTIVO");
        usuarioServices.actualizar(usuario);

        return ResponseEntity.ok(Map.of("mensaje", "Usuario desactivado correctamente"));
    }

    @PutMapping("/usuarios/{dni}/activar")
    public ResponseEntity<?> activarUsuario(@PathVariable Long dni) {
        Usuario usuario = usuarioServices.obtenerPorDni(dni);
        if (usuario == null) {
            throw new ResourceNotFoundException("Usuario", "DNI", dni);
        }

        usuario.setEstado("ACTIVO");
        usuarioServices.actualizar(usuario);

        return ResponseEntity.ok(Map.of("mensaje", "Usuario activado correctamente"));
    }

    @DeleteMapping("/usuarios/{dni}/eliminar")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long dni) {
        Usuario usuario = usuarioServices.obtenerPorDni(dni);
        if (usuario == null) {
            throw new ResourceNotFoundException("Usuario", "DNI", dni);
        }

        if ("ADMIN".equals(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "No se puede eliminar un usuario administrador"));
        }

        usuarioServices.eliminar_usuario(dni);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario eliminado correctamente"));
    }

    @GetMapping("/usuarios/estado/{estado}")
    public ResponseEntity<List<Usuario>> listarPorEstado(@PathVariable String estado) {
        List<Usuario> usuarios = usuarioServices.mostrar_usuario();
        List<Usuario> filtrados = usuarios.stream()
                .filter(u -> estado.equalsIgnoreCase(u.getEstado()))
                .toList();
        return ResponseEntity.ok(filtrados);
    }

    @GetMapping("/usuarios/estadisticas")
    public ResponseEntity<Map<String, Long>> estadisticasUsuarios() {
        List<Usuario> todos = usuarioServices.mostrar_usuario();
        List<Usuario> clientes = todos.stream()
            .filter(u -> "CLIENTE".equalsIgnoreCase(u.getRol()))
            .toList();
        long activos = clientes.stream().filter(u -> "ACTIVO".equalsIgnoreCase(u.getEstado())).count();
        long inactivos = clientes.stream().filter(u -> "INACTIVO".equalsIgnoreCase(u.getEstado())).count();
        long total = clientes.size();

        Map<String, Long> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("activos", activos);
        stats.put("inactivos", inactivos);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/personal")
    public ResponseEntity<List<Usuario>> listarPersonal() {
        List<Usuario> personal = usuarioServices.listarPersonal();
        return ResponseEntity.ok(personal);
    }

    @GetMapping("/personal/{dni}")
    public ResponseEntity<?> obtenerPersonal(@PathVariable Long dni) {
        Usuario usuario = usuarioServices.obtenerPorDni(dni);
        if (usuario == null || !"PERSONAL".equals(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Personal no encontrado"));
        }
        return ResponseEntity.ok(usuario);
    }

    @PostMapping("/personal")
    public ResponseEntity<?> crearPersonal(@Valid @RequestBody Usuario nuevoPersonal) {
        try {
            if (usuarioServices.existeDni(nuevoPersonal.getDni())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El DNI ya está registrado"));
            }
            if (usuarioServices.existeEmail(nuevoPersonal.getEmail())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El email ya está registrado"));
            }
            if (usuarioServices.existeTelefono(nuevoPersonal.getTelefono())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El teléfono ya está registrado"));
            }

            Usuario guardado = usuarioServices.crear_personal(nuevoPersonal);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al registrar personal: " + e.getMessage()));
        }
    }

    @PutMapping("/personal/{dni}")
    public ResponseEntity<?> actualizarPersonal(@PathVariable Long dni, @Valid @RequestBody com.gym.DTO.UsuarioUpdateDTO personalActualizado) {
        Usuario existente = usuarioServices.obtenerPorDni(dni);
        if (existente == null || !"PERSONAL".equals(existente.getRol())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Personal no encontrado"));
        }

        if (personalActualizado.getNombre() != null) existente.setNombre(personalActualizado.getNombre());
        if (personalActualizado.getApellido() != null) existente.setApellido(personalActualizado.getApellido());
        if (personalActualizado.getEmail() != null) existente.setEmail(personalActualizado.getEmail());
        if (personalActualizado.getTelefono() != null) existente.setTelefono(personalActualizado.getTelefono());
        if (personalActualizado.getEstado() != null) existente.setEstado(personalActualizado.getEstado());
        if (personalActualizado.getNewPassword() != null && !personalActualizado.getNewPassword().isEmpty()) {
            existente.setPasswordHash(passwordEncoder.encode(personalActualizado.getNewPassword()));
        }

        Usuario actualizado = usuarioServices.actualizar(existente);
        return ResponseEntity.ok(actualizado);
    }

    @PutMapping("/personal/{dni}/desactivar")
    public ResponseEntity<?> desactivarPersonal(@PathVariable Long dni) {
        Usuario existente = usuarioServices.obtenerPorDni(dni);
        if (existente == null || !"PERSONAL".equals(existente.getRol())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Personal no encontrado"));
        }

        existente.setEstado("INACTIVO");
        usuarioServices.actualizar(existente);
        return ResponseEntity.ok(Map.of("mensaje", "Personal desactivado correctamente"));
    }

    @DeleteMapping("/personal/{dni}")
    public ResponseEntity<?> eliminarPersonal(@PathVariable Long dni) {
        Usuario personal = usuarioServices.obtenerPorDni(dni);
        if (personal == null || !"PERSONAL".equals(personal.getRol())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Personal no encontrado"));
        }

        if ("ACTIVO".equalsIgnoreCase(personal.getEstado())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "No se puede eliminar un personal activo. Desactívelo primero."));
        }

        usuarioServices.eliminar_usuario(dni);
        return ResponseEntity.ok(Map.of("mensaje", "Personal eliminado correctamente"));
    }

    @GetMapping("/estadisticas/completas")
    public ResponseEntity<Map<String, Object>> estadisticasCompletas() {
        Map<String, Object> stats = new HashMap<>();

        List<Usuario> todosUsuarios = usuarioRepository.findAll();
        List<Usuario> clientes = todosUsuarios.stream()
            .filter(u -> "CLIENTE".equalsIgnoreCase(u.getRol()))
            .toList();
        long totalUsuarios = clientes.size();
        long usuariosActivos = clientes.stream().filter(u -> "ACTIVO".equalsIgnoreCase(u.getEstado())).count();
        long usuariosInactivos = todosUsuarios.stream().filter(u -> "INACTIVO".equalsIgnoreCase(u.getEstado())).count();
        long totalPersonal = usuarioRepository.countByRol("PERSONAL");

        stats.put("totalUsuarios", totalUsuarios);
        stats.put("usuariosActivos", usuariosActivos);
        stats.put("usuariosInactivos", usuariosInactivos);
        stats.put("totalPersonal", totalPersonal);

        long totalClases = claseRepository.count();
        List<Clase> todasClases = claseRepository.findAll();
        LocalDate hoyAdmin = LocalDate.now();
        long clasesActivas = todasClases.stream().filter(c -> {
            String est = c.getEstado() != null ? c.getEstado().toUpperCase() : "";
            if (!est.equals("ACTIVO") && !est.equals("ACTIVA")) return false;
            if (c.getFechaClase() == null) return true;
            LocalDate fecha = c.getFechaClase().toLocalDate();
            if (fecha.isBefore(hoyAdmin)) return false;
            if (fecha.isEqual(hoyAdmin) && c.getHoraf() != null && !c.getHoraf().isBlank()) {
                try {
                    java.time.LocalTime fin = java.time.LocalTime.parse(c.getHoraf().trim());
                    if (java.time.LocalTime.now().isAfter(fin)) return false;
                } catch (Exception e) { }
            }
            return true;
        }).count();
        stats.put("totalClases", totalClases);
        stats.put("clasesActivas", clasesActivas);

        long totalInscripciones = inscripcionClaseRepository.count();
        long inscripcionesActivas = inscripcionClaseRepository.findAll().stream()
                .filter(i -> "ACTIVA".equalsIgnoreCase(i.getEstado())).count();
        stats.put("totalInscripciones", totalInscripciones);
        stats.put("inscripcionesActivas", inscripcionesActivas);

        long membresiasActivas = MembresiaUsuarioRepository.countByEstado("ACTIVA");
        LocalDate hoy = LocalDate.now();
        LocalDate en7Dias = hoy.plusDays(7);
        long membresiasPorVencer = MembresiaUsuarioRepository.findByFechaFinBetween(hoy, en7Dias).stream()
                .filter(m -> "ACTIVA".equalsIgnoreCase(m.getEstado())).count();
        stats.put("membresiasActivas", membresiasActivas);
        stats.put("membresiasPorVencer", membresiasPorVencer);

        double ingresosTotales = pagoRepository.totalIngresos();
        double ingresosMesActual = pagoRepository.ingresosMesActual();
        long pagosCompletados = pagoRepository.countPagosCompletados();
        stats.put("ingresosTotales", ingresosTotales);
        stats.put("ingresosMesActual", ingresosMesActual);
        stats.put("pagosCompletados", pagosCompletados);

        long planBasico = MembresiaUsuarioRepository.countByIdMembresia(1L);
        long planPremium = MembresiaUsuarioRepository.countByIdMembresia(2L);
        stats.put("planBasico", planBasico);
        stats.put("planPremium", planPremium);

        LocalDate inicioMes = hoy.withDayOfMonth(1);
        long nuevosUsuariosMes = clientes.stream()
            .filter(u -> u.getFecha_registro() != null
                && !u.getFecha_registro().isBefore(inicioMes)
                && !u.getFecha_registro().isAfter(hoy))
            .count();
        stats.put("nuevosUsuariosMes", nuevosUsuariosMes);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/estadisticas/ingresos-mensuales")
    public ResponseEntity<List<Map<String, Object>>> ingresosMensuales() {
        List<Object[]> resultados = pagoRepository.ingresosMensuales();
        List<Map<String, Object>> lista = new ArrayList<>();

        for (Object[] fila : resultados) {
            Map<String, Object> item = new HashMap<>();
            int year = ((Number) fila[0]).intValue();
            int mes = ((Number) fila[1]).intValue();
            double monto = ((Number) fila[2]).doubleValue();
            item.put("mes", String.format("%d-%02d", year, mes));
            item.put("ingresos", monto);
            lista.add(item);
        }

        return ResponseEntity.ok(lista);
    }

    @GetMapping("/estadisticas/clases-populares")
    public ResponseEntity<List<Map<String, Object>>> clasesPopulares() {
        List<Clase> clases = claseRepository.findAll();
        List<Map<String, Object>> lista = new ArrayList<>();
        LocalDate hoyPop = LocalDate.now();

        for (Clase clase : clases) {
            String est = clase.getEstado() != null ? clase.getEstado().toUpperCase() : "";
            if ("FINALIZADA".equals(est) || "INACTIVO".equals(est)) continue;
            if (clase.getFechaClase() != null) {
                LocalDate fecha = clase.getFechaClase().toLocalDate();
                if (fecha.isBefore(hoyPop)) continue;
                if (fecha.isEqual(hoyPop) && clase.getHoraf() != null && !clase.getHoraf().isBlank()) {
                    try {
                        java.time.LocalTime fin = java.time.LocalTime.parse(clase.getHoraf().trim());
                        if (java.time.LocalTime.now().isAfter(fin)) continue;
                    } catch (Exception e) { }
                }
            }
            long inscripciones = inscripcionClaseRepository.countByIdClaseAndEstado(clase.getIdClase(), "ACTIVA");
            if (inscripciones == 0) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("idClase", clase.getIdClase());
            item.put("nombre", clase.getNombre());
            item.put("inscripciones", inscripciones);
            item.put("estado", clase.getEstado());
            lista.add(item);
        }

        lista.sort((a, b) -> Long.compare(((Number) b.get("inscripciones")).longValue(), ((Number) a.get("inscripciones")).longValue()));

        return ResponseEntity.ok(lista);
    }

    @GetMapping("/membresias")
    public ResponseEntity<List<Map<String, Object>>> listarMembresias() {
        List<MembresiaUsuario> membresias = MembresiaUsuarioRepository.findAll();

        List<Long> dnis = membresias.stream().map(MembresiaUsuario::getDni).distinct().toList();
        Map<Long, String> nombrePorDni = new HashMap<>();
        for (Long dni : dnis) {
            Usuario u = usuarioServices.obtenerPorDni(dni);
            nombrePorDni.put(dni, u != null ? u.getNombre() + " " + u.getApellido() : "Usuario " + dni);
        }

        List<Map<String, Object>> lista = new ArrayList<>();
        for (MembresiaUsuario m : membresias) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", m.getId());
            item.put("dni", m.getDni());
            item.put("idMembresia", m.getId_membresia());
            item.put("plan", m.getId_membresia() == 1 ? "Fit" : "Black");
            item.put("fechaInicio", m.getFecha_inicio());
            item.put("fechaFin", m.getFecha_fin());
            item.put("estado", m.getEstado());
            item.put("nombreUsuario", nombrePorDni.getOrDefault(m.getDni(), "Usuario " + m.getDni()));
            lista.add(item);
        }

        return ResponseEntity.ok(lista);
    }
}