package com.gym.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gym.models.InscripcionClase;
import com.gym.models.Clase;
import com.gym.repository.InscripcionClaseRepository;
import com.gym.repository.ClaseRepository;
import com.gym.exceptions.BadRequestException;
import com.gym.exceptions.ResourceNotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
public class InscripcionClaseService {

    @Autowired
    private InscripcionClaseRepository inscripcionRepository;

    @Autowired
    private ClaseRepository claseRepository;

    @Autowired
    private MembresiaUsuarioService membresiaService;

    private enum EstadoTemporalClase { FUTURA, EN_CURSO, PASADA }

    private EstadoTemporalClase obtenerEstadoTemporal(Clase clase) {
        if (clase.getFechaClase() == null) return EstadoTemporalClase.PASADA;

        LocalDate fechaClase = clase.getFechaClase().toLocalDate();
        LocalDate hoy = LocalDate.now();

        if (fechaClase.isBefore(hoy)) return EstadoTemporalClase.PASADA;
        if (fechaClase.isAfter(hoy)) return EstadoTemporalClase.FUTURA;

        LocalTime ahora = LocalTime.now();
        LocalTime horaInicio = null;
        LocalTime horaFin = null;

        if (clase.getHorai() != null && !clase.getHorai().isBlank()) {
            try { horaInicio = LocalTime.parse(clase.getHorai().trim(), DateTimeFormatter.ofPattern("HH:mm")); }
            catch (DateTimeParseException e) { }
        }
        if (clase.getHoraf() != null && !clase.getHoraf().isBlank()) {
            try { horaFin = LocalTime.parse(clase.getHoraf().trim(), DateTimeFormatter.ofPattern("HH:mm")); }
            catch (DateTimeParseException e) { }
        }

        if (horaFin != null && ahora.isAfter(horaFin)) return EstadoTemporalClase.PASADA;
        if (horaInicio != null && !ahora.isBefore(horaInicio)) return EstadoTemporalClase.EN_CURSO;

        return EstadoTemporalClase.FUTURA;
    }

    public List<InscripcionClase> getInscripcionesByUsuario(Long dniUsuario) {
        return inscripcionRepository.findByDniUsuario(dniUsuario);
    }

    public List<InscripcionClase> getInscripcionesActivasByUsuario(Long dniUsuario) {
        return inscripcionRepository.findByDniUsuarioAndEstado(dniUsuario, "ACTIVA");
    }

    public List<InscripcionClase> getInscripcionesByClase(Long idClase) {
        return inscripcionRepository.findByIdClase(idClase);
    }

    public long countInscripcionesByClase(Long idClase) {
        return inscripcionRepository.countByIdClaseAndEstado(idClase, "ACTIVA");
    }

    public boolean isInscrito(Long dniUsuario, Long idClase) {
        return inscripcionRepository.existsByDniUsuarioAndIdClaseAndEstado(dniUsuario, idClase, "ACTIVA");
    }

    @Transactional
    public synchronized InscripcionClase inscribirUsuario(Long dniUsuario, Long idClase) {
        Clase clase = claseRepository.findById(idClase)
                .orElseThrow(() -> new ResourceNotFoundException("Clase", "ID", idClase));

        String estado = clase.getEstado();
        if (!"ACTIVA".equalsIgnoreCase(estado) && !"ACTIVO".equalsIgnoreCase(estado)) {
            throw new BadRequestException("La clase no está disponible para inscripción");
        }

        EstadoTemporalClase estadoTemp = obtenerEstadoTemporal(clase);
        if (estadoTemp == EstadoTemporalClase.PASADA) {
            throw new BadRequestException("La clase ya finalizó. No puedes inscribirte a clases pasadas.");
        }
        if (estadoTemp == EstadoTemporalClase.EN_CURSO) {
            throw new BadRequestException("La clase ya inició. No puedes inscribirte a una clase en curso.");
        }

        if (membresiaService.obtenerMembresiActivaDelUsuario(dniUsuario) == null) {
            throw new BadRequestException("No tienes una membresía activa para inscribirte");
        }

        if (isInscrito(dniUsuario, idClase)) {
            throw new BadRequestException("Ya estás inscrito en esta clase");
        }

        if (clase.getFechaClase() != null && clase.getHorai() != null && clase.getHoraf() != null) {
            List<InscripcionClase> inscripcionesActivas = inscripcionRepository.findByDniUsuarioAndEstado(dniUsuario, "ACTIVA");
            for (InscripcionClase insc : inscripcionesActivas) {
                Clase otraClase = claseRepository.findById(insc.getIdClase()).orElse(null);
                if (otraClase == null || otraClase.getFechaClase() == null) continue;
                if (!clase.getFechaClase().equals(otraClase.getFechaClase())) continue;
                if (otraClase.getHorai() == null || otraClase.getHoraf() == null) continue;
                LocalTime ini1 = LocalTime.parse(clase.getHorai().trim(), DateTimeFormatter.ofPattern("HH:mm"));
                LocalTime fin1 = LocalTime.parse(clase.getHoraf().trim(), DateTimeFormatter.ofPattern("HH:mm"));
                LocalTime ini2 = LocalTime.parse(otraClase.getHorai().trim(), DateTimeFormatter.ofPattern("HH:mm"));
                LocalTime fin2 = LocalTime.parse(otraClase.getHoraf().trim(), DateTimeFormatter.ofPattern("HH:mm"));
                if (ini1.isBefore(fin2) && ini2.isBefore(fin1)) {
                    throw new BadRequestException("Ya tienes inscrita la clase \"" + otraClase.getNombre() + "\" en ese horario. No puedes estar en dos clases a la vez.");
                }
            }
        }

        long currentEnrollments = countInscripcionesByClase(idClase);
        int cupoMaximo = (clase.getCupo() != null && clase.getCupo() > 0) ? clase.getCupo() : 20;
        if (currentEnrollments >= cupoMaximo) {
            throw new BadRequestException("La clase está llena. No hay cupos disponibles.");
        }

        InscripcionClase inscripcion = new InscripcionClase();
        inscripcion.setDniUsuario(dniUsuario);
        inscripcion.setIdClase(idClase);
        inscripcion.setFechaInscripcion(LocalDateTime.now());
        inscripcion.setEstado("ACTIVA");

        return inscripcionRepository.save(inscripcion);
    }

    @Transactional
    public void cancelarInscripcion(Long dniUsuario, Long idClase) {
        Optional<InscripcionClase> inscripcion = inscripcionRepository.findByDniUsuarioAndIdClaseAndEstado(dniUsuario, idClase, "ACTIVA");

        if (inscripcion.isEmpty()) {
            throw new ResourceNotFoundException("Inscripción", "DNI y clase", dniUsuario + "/" + idClase);
        }

        Clase clase = claseRepository.findById(idClase).orElse(null);
        if (clase != null && obtenerEstadoTemporal(clase) == EstadoTemporalClase.EN_CURSO) {
            throw new BadRequestException("No puedes cancelar tu inscripción porque la clase está en curso.");
        }

        InscripcionClase insc = inscripcion.get();
        insc.setEstado("CANCELADA");
        inscripcionRepository.save(insc);
    }

    @Transactional
    public void eliminarInscripcion(Long idInscripcion) {
        if (!inscripcionRepository.existsById(idInscripcion)) {
            throw new ResourceNotFoundException("Inscripción", "ID", idInscripcion);
        }
        inscripcionRepository.deleteById(idInscripcion);
    }

    public int getCuposDisponibles(Long idClase) {
        Clase clase = claseRepository.findById(idClase).orElse(null);
        if (clase == null) return 0;
        int cupoMaximo = (clase.getCupo() != null && clase.getCupo() > 0) ? clase.getCupo() : 20;
        long currentEnrollments = countInscripcionesByClase(idClase);
        return (int) (cupoMaximo - currentEnrollments);
    }
}