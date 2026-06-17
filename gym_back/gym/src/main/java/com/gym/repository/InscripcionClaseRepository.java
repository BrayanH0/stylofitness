package com.gym.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gym.models.InscripcionClase;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscripcionClaseRepository extends JpaRepository<InscripcionClase, Long> {

    List<InscripcionClase> findByDniUsuario(Long dniUsuario);

    List<InscripcionClase> findByIdClase(Long idClase);

    Optional<InscripcionClase> findByDniUsuarioAndIdClase(Long dniUsuario, Long idClase);

    Optional<InscripcionClase> findByDniUsuarioAndIdClaseAndEstado(Long dniUsuario, Long idClase, String estado);

    long countByIdClase(Long idClase);

    long countByIdClaseAndEstado(Long idClase, String estado);

    boolean existsByDniUsuarioAndIdClase(Long dniUsuario, Long idClase);

    boolean existsByDniUsuarioAndIdClaseAndEstado(Long dniUsuario, Long idClase, String estado);

    List<InscripcionClase> findByDniUsuarioAndEstado(Long dniUsuario, String estado);
}