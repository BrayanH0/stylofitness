package com.gym.services;

import com.gym.models.Clase;
import com.gym.models.Usuario;
import com.gym.repository.ClaseRepository;
import com.gym.repository.InscripcionClaseRepository;
import com.gym.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Date;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClaseServiceTest {

    @Mock
    private ClaseRepository claseRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ClaseService claseService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Usuario crearTrainerPersonal(Long dni) {
        Usuario trainer = new Usuario();
        trainer.setDni(dni);
        trainer.setRol("PERSONAL");
        return trainer;
    }

    private Clase crearClase(Long id, Long trainerDni, String fecha, String horai, String horaf) {
        Clase clase = new Clase();
        clase.setIdClase(id);
        clase.setIdTrainer(trainerDni);
        clase.setFechaClase(Date.valueOf(fecha));
        clase.setHorai(horai);
        clase.setHoraf(horaf);
        clase.setNombre("Test");
        clase.setCupo(10);
        clase.setEstado("ACTIVO");
        return clase;
    }

    @Test
    @DisplayName("Debe rechazar creacion de clase solapada con existente del mismo trainer")
    void testCreateClaseRechazaSolapamiento() {
        Long trainerDni = 88888888L;
        Usuario trainer = crearTrainerPersonal(trainerDni);
        when(usuarioRepository.findById(trainerDni)).thenReturn(java.util.Optional.of(trainer));

        Clase existente = crearClase(1L, trainerDni, "2099-12-31", "10:00", "11:00");
        when(claseRepository.findByIdTrainerAndFechaClase(trainerDni, Date.valueOf("2099-12-31")))
                .thenReturn(Arrays.asList(existente));

        when(claseRepository.save(any(Clase.class))).thenAnswer(inv -> inv.getArgument(0));

        Clase nueva = crearClase(null, trainerDni, "2099-12-31", "10:30", "11:30");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> claseService.createClase(nueva));
        assertTrue(ex.getMessage().toLowerCase().contains("solapa"));
        verify(claseRepository, never()).save(any(Clase.class));
    }

    @Test
    @DisplayName("Debe rechazar clase con duracion menor a 30 minutos")
    void testCreateClaseRechazaDuracionMenor30Min() {
        Long trainerDni = 88888888L;
        Usuario trainer = crearTrainerPersonal(trainerDni);
        when(usuarioRepository.findById(trainerDni)).thenReturn(java.util.Optional.of(trainer));

        Clase clase = crearClase(null, trainerDni, "2099-12-31", "10:00", "10:15");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> claseService.createClase(clase));
        assertTrue(ex.getMessage().contains("30 minutos"));
        verify(claseRepository, never()).save(any(Clase.class));
    }
}
