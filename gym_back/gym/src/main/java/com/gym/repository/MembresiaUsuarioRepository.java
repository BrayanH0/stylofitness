package com.gym.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gym.models.MembresiaUsuario;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MembresiaUsuarioRepository extends JpaRepository<MembresiaUsuario, Long> {
    List<MembresiaUsuario> findByDni(Long dni);

    long countByEstado(String estado);

    @Query("SELECT m FROM MembresiaUsuario m WHERE m.fecha_fin BETWEEN :start AND :end")
    List<MembresiaUsuario> findByFechaFinBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COUNT(m) FROM MembresiaUsuario m WHERE m.id_membresia = :idMembresia")
    long countByIdMembresia(@Param("idMembresia") Long idMembresia);
}