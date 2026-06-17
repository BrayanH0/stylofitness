package com.gym.services;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.gym.models.MembresiaUsuario;
import com.gym.models.Usuario;
import com.gym.repository.MembresiaUsuarioRepository;
import com.gym.repository.UsuarioRepository;
import org.springframework.stereotype.Service;


@Service
public class MembresiaUsuarioService {
    @Autowired
    private MembresiaUsuarioRepository repo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    private static final Logger logger = LoggerFactory.getLogger(MembresiaUsuarioService.class);

    @Transactional
    public void crearMembresia(Long dni, Long idMembresia) {
        crearMembresiaConDuracion(dni, idMembresia, 1);
    }

    @Transactional
    public synchronized void crearMembresiaConDuracion(Long dni, Long idMembresia, int duracionMeses) {
        Usuario usuario = usuarioRepo.findByDni(dni);
        if (usuario == null) {
            throw new RuntimeException("Usuario con DNI " + dni + " no encontrado");
        }

        MembresiaUsuario existente = obtenerMembresiActivaDelUsuario(dni);
        if (existente != null) {
            logger.warn("Usuario DNI {} ya tiene membresia activa (id={}). No se crea una nueva.",
                        dni, existente.getId());
            return;
        }

        MembresiaUsuario m = new MembresiaUsuario();
        m.setDni(dni);
        m.setId_membresia(idMembresia);
        m.setFecha_inicio(LocalDate.now());
        m.setFecha_fin(LocalDate.now().plusMonths(duracionMeses));
        m.setEstado("ACTIVA");

        repo.save(m);
        logger.info("Membresia creada: dni={}, plan={}, duracion={} meses, fin={}",
                    dni, idMembresia, duracionMeses, m.getFecha_fin());
    }

    @Transactional
    public synchronized void crearMembresiaPorPlan(Long dni, String plan) {
        Usuario usuario = usuarioRepo.findByDni(dni);
        if (usuario == null) {
            throw new RuntimeException("Usuario con DNI " + dni + " no encontrado");
        }

        MembresiaUsuario existente = obtenerMembresiActivaDelUsuario(dni);
        if (existente != null) {
            return;
        }

        MembresiaUsuario m = new MembresiaUsuario();
        m.setDni(dni);
        m.setFecha_inicio(LocalDate.now());
        m.setEstado("ACTIVA");

        if ("basic".equalsIgnoreCase(plan) || "fit_1m".equalsIgnoreCase(plan)) {
            m.setId_membresia(1L);
            m.setFecha_fin(LocalDate.now().plusMonths(1));
        } else if ("premium".equalsIgnoreCase(plan) || "black_3m".equalsIgnoreCase(plan)) {
            m.setId_membresia(2L);
            m.setFecha_fin(LocalDate.now().plusMonths(3));
        } else if ("fit_3m".equalsIgnoreCase(plan)) {
            m.setId_membresia(1L);
            m.setFecha_fin(LocalDate.now().plusMonths(3));
        } else if ("black_1m".equalsIgnoreCase(plan)) {
            m.setId_membresia(2L);
            m.setFecha_fin(LocalDate.now().plusMonths(1));
        } else {
            m.setId_membresia(1L);
            m.setFecha_fin(LocalDate.now().plusMonths(1));
        }

        repo.save(m);
    }

    public List<MembresiaUsuario> obtenerMembresiasDelUsuario(Long dni) {
        try {
            return repo.findByDni(dni);
        } catch (Exception e) {
            return List.of();
        }
    }

    public MembresiaUsuario obtenerMembresiActivaDelUsuario(Long dni) {
        try {
            List<MembresiaUsuario> membresias = repo.findByDni(dni);
            LocalDate hoy = LocalDate.now();

            return membresias.stream()
                .filter(m -> "ACTIVA".equalsIgnoreCase(m.getEstado()))
                .filter(m -> m.getFecha_fin() != null && !m.getFecha_fin().isBefore(hoy))
                .filter(m -> m.getFecha_inicio() != null)
                .max((m1, m2) -> m1.getFecha_inicio().compareTo(m2.getFecha_inicio()))
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}