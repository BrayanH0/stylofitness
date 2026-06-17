package com.gym.services;

import com.gym.models.Usuario;
import com.gym.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Usuario crearUsuarioValido() {
        Usuario u = new Usuario();
        u.setDni(12345678L);
        u.setNombre("Juan");
        u.setApellido("Perez");
        u.setEmail("juan@test.com");
        u.setTelefono("912345678");
        u.setDireccion("Av. Principal 123");
        u.setPasswordHash("Password1");
        u.setFecha_nacimiento("1990-01-01");
        u.setEstado("INACTIVO");
        u.setRol("CLIENTE");
        return u;
    }

    @Test
    @DisplayName("Debe rechazar creacion de usuario con DNI duplicado")
    void testCrearUsuarioRechazaDNIDuplicado() {
        Usuario u = crearUsuarioValido();
        when(usuarioRepository.existsByDni(u.getDni())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.crear_usuario(u));
        assertTrue(ex.getMessage().toLowerCase().contains("dni"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe rechazar login de usuario con estado INACTIVO")
    void testLoginUsuarioInactivo() {
        Long dni = 12345678L;
        String rawPassword = "Password1";

        Usuario u = crearUsuarioValido();
        u.setEstado("INACTIVO");
        u.setPasswordHash(rawPassword);

        when(usuarioRepository.findByDni(dni)).thenReturn(u);
        when(passwordEncoder.matches(rawPassword, rawPassword)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> usuarioService.login(dni, rawPassword));
        assertTrue(ex.getMessage().toLowerCase().contains("inactivo"));
    }
}
