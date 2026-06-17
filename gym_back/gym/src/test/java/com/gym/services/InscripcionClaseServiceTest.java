package com.gym.services;

import com.gym.exceptions.BadRequestException;
import com.gym.models.Clase;
import com.gym.models.InscripcionClase;
import com.gym.models.MembresiaUsuario;
import com.gym.repository.ClaseRepository;
import com.gym.repository.InscripcionClaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InscripcionClaseServiceTest {

    @Mock
    private InscripcionClaseRepository inscripcionRepository;

    @Mock
    private ClaseRepository claseRepository;

    @Mock
    private MembresiaUsuarioService membresiaService;

    @InjectMocks
    private InscripcionClaseService inscripcionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Clase crearClaseFutura(Long id, int cupo) {
        Clase clase = new Clase();
        clase.setIdClase(id);
        clase.setFechaClase(Date.valueOf(LocalDate.now().plusDays(7)));
        clase.setHorai("10:00");
        clase.setHoraf("11:00");
        clase.setNombre("Yoga");
        clase.setCupo(cupo);
        clase.setEstado("ACTIVA");
        clase.setIdTrainer(88888888L);
        return clase;
    }

    @Test
    @DisplayName("Debe rechazar inscripcion si usuario no tiene membresia activa")
    void testInscribirSinMembresia() {
        Long dniUsuario = 33333333L;
        Long idClase = 1L;

        Clase clase = crearClaseFutura(idClase, 20);
        when(claseRepository.findById(idClase)).thenReturn(Optional.of(clase));
        when(membresiaService.obtenerMembresiActivaDelUsuario(dniUsuario)).thenReturn(null);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> inscripcionService.inscribirUsuario(dniUsuario, idClase));
        assertTrue(ex.getMessage().toLowerCase().contains("membresía") || ex.getMessage().toLowerCase().contains("membresia"));
        verify(inscripcionRepository, never()).save(any(InscripcionClase.class));
    }

    @Test
    @DisplayName("Debe rechazar inscripcion si clase esta llena")
    void testInscribirClaseLlena() {
        Long dniUsuario = 33333333L;
        Long idClase = 1L;

        Clase clase = crearClaseFutura(idClase, 5);
        when(claseRepository.findById(idClase)).thenReturn(Optional.of(clase));

        MembresiaUsuario membresia = new MembresiaUsuario();
        membresia.setEstado("ACTIVA");
        when(membresiaService.obtenerMembresiActivaDelUsuario(dniUsuario)).thenReturn(membresia);

        when(inscripcionRepository.existsByDniUsuarioAndIdClaseAndEstado(dniUsuario, idClase, "ACTIVA")).thenReturn(false);
        when(inscripcionRepository.countByIdClaseAndEstado(idClase, "ACTIVA")).thenReturn(5L);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> inscripcionService.inscribirUsuario(dniUsuario, idClase));
        assertTrue(ex.getMessage().toLowerCase().contains("llena") || ex.getMessage().toLowerCase().contains("cupo"));
        verify(inscripcionRepository, never()).save(any(InscripcionClase.class));
    }

}
