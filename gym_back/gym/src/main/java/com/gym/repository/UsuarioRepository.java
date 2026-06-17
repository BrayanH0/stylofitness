package com.gym.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gym.models.Usuario;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface UsuarioRepository extends JpaRepository<com.gym.models.Usuario, Long> {

    boolean existsByDni(Long dni);

    boolean existsByEmail(String email);

    boolean existsByTelefono(String telefono);

    @Query(value = "SELECT * FROM USUARIO WHERE dni = :dni", nativeQuery = true)
    Usuario findByDni(Long dni);

    List<Usuario> findByRol(String rol);

    long countByEstado(String estado);

    long countByRol(String rol);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.fecha_registro BETWEEN :start AND :end")
    long countByFechaRegistroBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}