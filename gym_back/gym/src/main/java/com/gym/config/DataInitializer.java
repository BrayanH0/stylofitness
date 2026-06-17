package com.gym.config;

import com.gym.models.Usuario;
import com.gym.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUsers(UsuarioRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (repo.findByDni(99999999L) == null) {
                Usuario admin = new Usuario();
                admin.setDni(99999999L);
                admin.setNombre("Admin");
                admin.setApellido("Principal");
                admin.setTelefono("999999999");
                admin.setEmail("admin@stylofitness.com");
                admin.setFecha_nacimiento("1990-01-01");
                admin.setDireccion("Sede Central");
                admin.setFecha_registro(LocalDate.now());
                admin.setEstado("ACTIVO");
                admin.setPasswordHash(encoder.encode("Admin123!"));
                admin.setRol("ADMIN");
                admin.setFecha_contratacion(LocalDate.now());
                repo.save(admin);
                System.out.println("[DataInitializer] Usuario ADMIN creado: DNI 99999999");
            } else {
                System.out.println("[DataInitializer] Usuario ADMIN ya existe");
            }

            if (repo.findByDni(88888888L) == null) {
                Usuario personal = new Usuario();
                personal.setDni(88888888L);
                personal.setNombre("Carlos");
                personal.setApellido("Entrenador");
                personal.setTelefono("988888888");
                personal.setEmail("personal@stylofitness.com");
                personal.setFecha_nacimiento("1985-06-15");
                personal.setDireccion("Sede Central");
                personal.setFecha_registro(LocalDate.now());
                personal.setEstado("ACTIVO");
                personal.setPasswordHash(encoder.encode("Personal123!"));
                personal.setRol("PERSONAL");
                personal.setFecha_contratacion(LocalDate.now());
                repo.save(personal);
                System.out.println("[DataInitializer] Usuario PERSONAL creado: DNI 88888888");
            } else {
                System.out.println("[DataInitializer] Usuario PERSONAL ya existe");
            }
        };
    }
}
