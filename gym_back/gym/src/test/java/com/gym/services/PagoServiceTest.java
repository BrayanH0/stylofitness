package com.gym.services;

import com.gym.models.Pago;
import com.gym.repository.PagoRepository;
import com.gym.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class PagoServiceTest {

    @Mock
    private PagoRepository repo;

    @Mock
    private UsuarioRepository usuarioRepo;

    @InjectMocks
    private PagoService pagoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("No debe guardar Pago duplicado con mismo sessionId")
    void testGuardarPagoManualEvitaDuplicado() {
        String sessionId = "ses_123";
        when(repo.existsBySessionId(sessionId)).thenReturn(true);

        pagoService.guardarPagoManual(sessionId, "pi_123", 6990, "test@test.com", "fit_1m", "12345678");

        verify(repo, never()).save(any(Pago.class));
    }

}
