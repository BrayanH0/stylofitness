package com.gym.services;

import com.gym.models.Clase;
import com.gym.models.InscripcionClase;
import com.gym.models.Usuario;
import com.gym.repository.ClaseRepository;
import com.gym.repository.InscripcionClaseRepository;
import com.gym.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class ClaseService {

    private static final Logger logger = LoggerFactory.getLogger(ClaseService.class);

    private final ClaseRepository claseRepository;
    private final InscripcionClaseRepository inscripcionRepository;
    private final UsuarioRepository usuarioRepository;

    public ClaseService(ClaseRepository claseRepository,
                        InscripcionClaseRepository inscripcionRepository,
                        UsuarioRepository usuarioRepository) {
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<Clase> getAllClases() {
        return claseRepository.findAll();
    }

    public List<Clase> getClasesByTrainer(Long idTrainer) {
        return claseRepository.findByIdTrainer(idTrainer);
    }

    public Clase getClaseById(Long id) {
        return claseRepository.findById(id).orElse(null);
    }

    @Transactional
    public Clase createClase(Clase clase) {
        logger.info("Creando clase: {}, horario={}", clase.getNombre(), clase.getHorario());

        if (clase.getIdTrainer() != null) {
            Usuario trainer = usuarioRepository.findById(clase.getIdTrainer()).orElse(null);
            if (trainer == null || !"PERSONAL".equalsIgnoreCase(trainer.getRol())) {
                throw new IllegalArgumentException("El trainer no existe o no tiene rol PERSONAL");
            }
        }

        if (clase.getFechaClase() != null) {
            LocalDate fechaClase = clase.getFechaClase().toLocalDate();
            if (fechaClase.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("No se pueden crear clases en fechas pasadas");
            }
            if (fechaClase.isEqual(LocalDate.now())) {
                if (clase.getHorai() != null && !clase.getHorai().isBlank()) {
                    LocalTime inicio = LocalTime.parse(clase.getHorai());
                    if (!inicio.isAfter(LocalTime.now())) {
                        throw new IllegalArgumentException("La hora de inicio debe ser posterior a la hora actual");
                    }
                }
            }
        }

        if (clase.getHorai() != null && !clase.getHorai().isBlank()
                && clase.getHoraf() != null && !clase.getHoraf().isBlank()) {
            LocalTime inicio = LocalTime.parse(clase.getHorai());
            LocalTime fin = LocalTime.parse(clase.getHoraf());
            if (inicio.isBefore(LocalTime.of(6, 0))) {
                throw new IllegalArgumentException("Las clases no pueden comenzar antes de las 06:00");
            }
            if (!fin.isAfter(inicio)) {
                throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
            }
            long duracionMin = java.time.Duration.between(inicio, fin).toMinutes();
            if (duracionMin < 30) {
                throw new IllegalArgumentException("La clase debe durar al menos 30 minutos");
            }
        }

        validarSolapamiento(clase.getIdTrainer(), clase.getFechaClase(), clase.getHorai(), clase.getHoraf(), null);

        String estado = clase.getEstado();
        if (estado == null || estado.isBlank()) {
            clase.setEstado("ACTIVO");
        } else {
            clase.setEstado(estado.toUpperCase());
        }

        Clase saved = claseRepository.save(clase);
        logger.info("Clase guardada con id: {}", saved.getIdClase());
        return saved;
    }

    public Clase updateClase(Long id, Clase data) {
        Clase clase = claseRepository.findById(id).orElse(null);

        if (clase == null) return null;

        if (data.getIdTrainer() != null) {
            Usuario trainer = usuarioRepository.findById(data.getIdTrainer()).orElse(null);
            if (trainer == null || !"PERSONAL".equalsIgnoreCase(trainer.getRol())) {
                throw new IllegalArgumentException("El trainer no existe o no tiene rol PERSONAL");
            }
        }

        if (data.getFechaClase() != null) {
            LocalDate fechaClase = data.getFechaClase().toLocalDate();
            if (fechaClase.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("No se pueden asignar fechas pasadas a la clase");
            }
            if (fechaClase.isEqual(LocalDate.now())) {
                String hi = data.getHorai() != null ? data.getHorai() : clase.getHorai();
                if (hi != null && !hi.isBlank()) {
                    LocalTime inicio = LocalTime.parse(hi);
                    if (!inicio.isAfter(LocalTime.now())) {
                        throw new IllegalArgumentException("La hora de inicio debe ser posterior a la hora actual");
                    }
                }
            }
        }

        String hIni = data.getHorai() != null ? data.getHorai() : clase.getHorai();
        String hFin = data.getHoraf() != null ? data.getHoraf() : clase.getHoraf();
        if (hIni != null && !hIni.isBlank() && hFin != null && !hFin.isBlank()) {
            LocalTime inicio = LocalTime.parse(hIni);
            LocalTime fin = LocalTime.parse(hFin);
            if (inicio.isBefore(LocalTime.of(6, 0))) {
                throw new IllegalArgumentException("Las clases no pueden comenzar antes de las 06:00");
            }
            if (!fin.isAfter(inicio)) {
                throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
            }
            long duracionMin = java.time.Duration.between(inicio, fin).toMinutes();
            if (duracionMin < 30) {
                throw new IllegalArgumentException("La clase debe durar al menos 30 minutos");
            }
        }

        Long trainerId = data.getIdTrainer() != null ? data.getIdTrainer() : clase.getIdTrainer();
        java.sql.Date fecha = data.getFechaClase() != null ? data.getFechaClase() : clase.getFechaClase();
        String hIniUpd = data.getHorai() != null ? data.getHorai() : clase.getHorai();
        String hFinUpd = data.getHoraf() != null ? data.getHoraf() : clase.getHoraf();
        validarSolapamiento(trainerId, fecha, hIniUpd, hFinUpd, id);

        clase.setNombre(data.getNombre());
        clase.setFechaClase(data.getFechaClase());
        clase.setHorario(data.getHorario());
        clase.setIdTrainer(data.getIdTrainer());
        if (data.getEstado() != null && !data.getEstado().isBlank()) {
            clase.setEstado(data.getEstado().toUpperCase());
        }
        if (data.getHorai() != null) clase.setHorai(data.getHorai());
        if (data.getHoraf() != null) clase.setHoraf(data.getHoraf());
        if (data.getCupo() != null) clase.setCupo(data.getCupo());

        return claseRepository.save(clase);
    }

    @Transactional
    public boolean deleteClase(Long id) {
        Optional<Clase> opt = claseRepository.findById(id);
        if (!opt.isPresent()) return false;
        Clase clase = opt.get();
        clase.setEstado("INACTIVO");
        claseRepository.save(clase);

        List<InscripcionClase> inscripciones = inscripcionRepository.findByIdClase(id);
        for (InscripcionClase insc : inscripciones) {
            if ("ACTIVA".equalsIgnoreCase(insc.getEstado())) {
                insc.setEstado("CANCELADA");
                inscripcionRepository.save(insc);
                logger.info("Inscripcion {} cancelada por eliminacion de clase {}", insc.getIdInscripcion(), id);
            }
        }
        return true;
    }

    private void validarSolapamiento(Long trainerId, java.sql.Date fecha, String horai, String horaf, Long excludeId) {
        if (trainerId == null || fecha == null || horai == null || horai.isBlank()
                || horaf == null || horaf.isBlank()) {
            return;
        }
        List<Clase> clasesDelTrainer = claseRepository.findByIdTrainerAndFechaClase(trainerId, fecha);
        for (Clase existente : clasesDelTrainer) {
            if (excludeId != null && existente.getIdClase().equals(excludeId)) {
                continue;
            }
            if (horariosSeSolapan(horai, horaf, existente.getHorai(), existente.getHoraf())) {
                throw new IllegalArgumentException("El horario de la clase se solapa con otra clase existente del mismo trainer");
            }
        }
    }

    private boolean horariosSeSolapan(String horai1, String horaf1, String horai2, String horaf2) {
        if (horai1 == null || horaf1 == null || horai2 == null || horaf2 == null) {
            return false;
        }
        LocalTime inicio1 = LocalTime.parse(horai1);
        LocalTime fin1 = LocalTime.parse(horaf1);
        LocalTime inicio2 = LocalTime.parse(horai2);
        LocalTime fin2 = LocalTime.parse(horaf2);
        return inicio1.isBefore(fin2) && inicio2.isBefore(fin1);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void finalizarClasesPasadas() {
        LocalDate hoy = LocalDate.now();
        Date sqlDate = Date.valueOf(hoy);

        List<Clase> clasesPasadas = claseRepository.findByFechaClaseBeforeAndEstadoEquals(sqlDate, "ACTIVO");
        for (Clase clase : clasesPasadas) {
            clase.setEstado("FINALIZADA");
            claseRepository.save(clase);
            logger.info("Clase {} ({}) marcada como FINALIZADA (fecha pasada)", clase.getIdClase(), clase.getNombre());
            List<InscripcionClase> inscripciones = inscripcionRepository.findByIdClase(clase.getIdClase());
            for (InscripcionClase insc : inscripciones) {
                if ("ACTIVA".equalsIgnoreCase(insc.getEstado())) {
                    insc.setEstado("FINALIZADA");
                    inscripcionRepository.save(insc);
                }
            }
        }

        List<Clase> clasesHoy = claseRepository.findByFechaClaseAndEstadoEquals(sqlDate, "ACTIVO");
        LocalTime ahora = LocalTime.now();
        for (Clase clase : clasesHoy) {
            String hf = clase.getHoraf();
            if (hf != null && !hf.isBlank()) {
                LocalTime fin = LocalTime.parse(hf);
                if (!fin.isAfter(ahora)) {
                    clase.setEstado("FINALIZADA");
                    claseRepository.save(clase);
                    logger.info("Clase {} ({}) marcada como FINALIZADA (hora fin {} <= {})",
                            clase.getIdClase(), clase.getNombre(), fin, ahora);
                    List<InscripcionClase> inscripcionesHoy = inscripcionRepository.findByIdClase(clase.getIdClase());
                    for (InscripcionClase insc : inscripcionesHoy) {
                        if ("ACTIVA".equalsIgnoreCase(insc.getEstado())) {
                            insc.setEstado("FINALIZADA");
                            inscripcionRepository.save(insc);
                        }
                    }
                }
            }
        }
    }
}
