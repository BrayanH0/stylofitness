package com.gym.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.gym.models.Clase;

@Repository
public interface ClaseRepository extends JpaRepository<Clase, Long> {
    List<Clase> findByIdTrainer(Long idTrainer);

    List<Clase> findByIdTrainerAndFechaClase(Long idTrainer, java.sql.Date fechaClase);

    List<Clase> findByFechaClaseBeforeAndEstadoEquals(java.sql.Date fecha, String estado);

    List<Clase> findByFechaClaseAndEstadoEquals(java.sql.Date fecha, String estado);
}
