package com.gym.services;

import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.gym.models.Pago;
import com.gym.models.Usuario;
import com.gym.repository.PagoRepository;
import com.gym.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PagoService {

    private static final Logger logger = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository repo;
    private final UsuarioRepository usuarioRepo;

    public PagoService(PagoRepository repo, UsuarioRepository usuarioRepo) {
        this.repo = repo;
        this.usuarioRepo = usuarioRepo;
    }

    @Transactional
    public synchronized void guardarPagoDeSesion(Session session) {
        if (repo.existsBySessionId(session.getId())) {
            logger.info("Pago ya registrado para session: {}, omitiendo duplicado", session.getId());
            return;
        }

        Pago Pago = new Pago();

        Pago.setSessionId(session.getId());
        Pago.setPaymentIntentId(session.getPaymentIntent());
        Pago.setMonto(session.getAmountTotal() / 100.0);
        Pago.setMoneda(session.getCurrency());
        Pago.setEmail(session.getCustomerDetails() != null ? session.getCustomerDetails().getEmail() : "no-email");
        Pago.setFecha(LocalDateTime.now());
        Pago.setEstado("COMPLETADO");

        if (session.getMetadata() != null) {
            String userId = session.getMetadata().get("userId");
            if (userId == null) userId = session.getMetadata().get("dni");
            if (userId != null) {
                Pago.setUserId(userId);
            }
            String plan = session.getMetadata().get("plan");
            if (plan != null) {
                Pago.setPlan(plan);
            }
        }

        repo.save(Pago);
        logger.info("Pago guardado desde Session");
    }

    public void guardarPagoDePaymentIntent(PaymentIntent pi) {
        Pago Pago = new Pago();

        Pago.setPaymentIntentId(pi.getId());
        Pago.setMonto(pi.getAmount() / 100.0);
        Pago.setMoneda(pi.getCurrency());
        Pago.setEmail(pi.getReceiptEmail() != null ? pi.getReceiptEmail() : "no-email");
        Pago.setFecha(LocalDateTime.now());
        Pago.setEstado("COMPLETADO");

        repo.save(Pago);
        logger.info("Pago guardado desde PaymentIntent");
    }

    @Transactional
    public synchronized void guardarPagoManual(String sessionId, String paymentIntentId, int amount, String email) {
        if (repo.existsBySessionId(sessionId)) {
            logger.info("Pago ya registrado para session: {}, omitiendo duplicado", sessionId);
            return;
        }

        Pago Pago = new Pago();
        Pago.setSessionId(sessionId);
        Pago.setPaymentIntentId(paymentIntentId);
        Pago.setMonto(amount / 100.0);
        Pago.setEmail(email);
        Pago.setEstado("COMPLETADO");
        Pago.setFecha(LocalDateTime.now());

        repo.save(Pago);
        logger.info("Pago guardado en Oracle correctamente");
    }

    @Transactional
    public synchronized void guardarPagoManual(String sessionId, String paymentIntentId, int amount, String email, String plan) {
        if (repo.existsBySessionId(sessionId)) {
            logger.info("Pago ya registrado para session: {}, omitiendo duplicado", sessionId);
            return;
        }

        Pago Pago = new Pago();
        Pago.setSessionId(sessionId);
        Pago.setPaymentIntentId(paymentIntentId);
        Pago.setMonto(amount / 100.0);
        Pago.setEmail(email);
        Pago.setEstado("COMPLETADO");
        Pago.setFecha(LocalDateTime.now());
        Pago.setPlan(plan);

        repo.save(Pago);
        logger.info("Pago guardado en Oracle correctamente (plan: {})", plan);
    }

    @Transactional
    public synchronized void guardarPagoManual(String sessionId, String paymentIntentId, int amount, String email, String plan, String dniStr) {
        if (repo.existsBySessionId(sessionId)) {
            logger.info("Pago ya registrado para session: {}, omitiendo duplicado", sessionId);
            return;
        }

        Pago Pago = new Pago();
        Pago.setSessionId(sessionId);
        Pago.setPaymentIntentId(paymentIntentId);
        Pago.setMonto(amount / 100.0);
        Pago.setEmail(email);
        Pago.setEstado("COMPLETADO");
        Pago.setFecha(LocalDateTime.now());
        Pago.setPlan(plan);

        if (dniStr != null) {
            Pago.setUserId(dniStr);
        }

        repo.save(Pago);
        logger.info("Pago guardado en Oracle correctamente (plan: {}, dni: {})", plan, dniStr);
    }

    public void guardarPagoDePaymentIntentFromWebhook(String id, int amount, String currency, String email, String status) {
        Pago Pago = new Pago();
        Pago.setPaymentIntentId(id);
        Pago.setMonto(amount / 100.0);
        Pago.setMoneda(currency);
        Pago.setEmail(email);
        Pago.setFecha(LocalDateTime.now());
        Pago.setEstado("COMPLETADO");

        repo.save(Pago);
        logger.info("Pago guardado desde PaymentIntent (webhook)");
    }

    @Transactional
    public void activarUsuarioPorDni(Long dni) {
        Usuario usuario = usuarioRepo.findByDni(dni);
        if (usuario == null) {
            logger.warn("Usuario con DNI {} no encontrado. No se pudo activar la cuenta.", dni);
            return;
        }
        usuario.setEstado("ACTIVO");
        usuarioRepo.save(usuario);
        logger.info("Usuario DNI {} activado correctamente", dni);
    }

    public Pago obtenerUltimoPagoPorDni(String dniStr) {
        try {
            return repo.findTopByUserIdOrderByFechaDesc(dniStr);
        } catch (Exception e) {
            logger.error("Error obteniendo ultimo Pago para DNI {}: {}", dniStr, e.getMessage());
            return null;
        }
    }

    public List<Pago> obtenerTodosPagos() {
        try {
            return repo.findAll();
        } catch (Exception e) {
            logger.error("Error obteniendo todos los pagos: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
}