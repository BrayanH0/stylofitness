package com.gym.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.gym.models.Pago;
import java.util.List;


public interface PagoRepository extends JpaRepository<Pago, Long> {
	Pago findTopByUserIdOrderByFechaDesc(String userId);

	boolean existsBySessionId(String sessionId);

	long countByEstado(String estado);

	@Query("SELECT COUNT(p) FROM Pago p WHERE UPPER(p.estado) IN ('COMPLETADO', 'COMPLETED', 'SUCCEEDED')")
	long countPagosCompletados();

	@Query("SELECT YEAR(p.fecha), MONTH(p.fecha), SUM(p.monto) FROM Pago p WHERE UPPER(p.estado) IN ('COMPLETADO', 'COMPLETED', 'SUCCEEDED') GROUP BY YEAR(p.fecha), MONTH(p.fecha) ORDER BY YEAR(p.fecha) DESC, MONTH(p.fecha) DESC")
	List<Object[]> ingresosMensuales();

	@Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE UPPER(p.estado) IN ('COMPLETADO', 'COMPLETED', 'SUCCEEDED')")
	double totalIngresos();

	@Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE UPPER(p.estado) IN ('COMPLETADO', 'COMPLETED', 'SUCCEEDED') AND YEAR(p.fecha) = YEAR(CURRENT_DATE) AND MONTH(p.fecha) = MONTH(CURRENT_DATE)")
	double ingresosMesActual();

}
