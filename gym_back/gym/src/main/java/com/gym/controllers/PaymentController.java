package com.gym.controllers;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.Map;
import com.gym.models.Pago;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.gym.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private com.gym.services.MembresiaUsuarioService membresiaService;
    @Autowired
    private com.gym.services.PagoService pagoService;
    @Autowired
    private JwtUtil jwtUtil;

    private Long getAuthenticatedDni(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.extractDni(token);
        }
        return null;
    }

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public PaymentController(@Value("${stripe.secret.key}") String stripeSecretKey) {
        Stripe.apiKey = stripeSecretKey;
    }

    @PostMapping("/create-checkout-session/{plan}")
    public ResponseEntity<?> createCheckoutSession(@PathVariable String plan, @RequestParam(required = false) Long dni) {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            logger.error("APP_FRONTEND_URL no configurada. Valor actual: {}", frontendUrl);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error de configuracion del servidor. Contacte a soporte."));
        }
        try {
            long precio = 0;
            String nombreMembresia = "";

            switch (plan) {
                case "fit_1m":
                    precio = 6990L;
                    nombreMembresia = "PLAN Fit - 1 Mes - S/ 69.90 (incl. IGV)";
                    break;
                case "fit_3m":
                    precio = 20970L;
                    nombreMembresia = "PLAN Fit - 3 Meses - S/ 209.70 (incl. IGV)";
                    break;
                case "black_1m":
                    precio = 8990L;
                    nombreMembresia = "PLAN Black - 1 Mes - S/ 89.90 (incl. IGV)";
                    break;
                case "black_3m":
                    precio = 26970L;
                    nombreMembresia = "PLAN Black - 3 Meses - S/ 269.70 (incl. IGV)";
                    break;
                case "basic":
                    precio = 8142L;
                    nombreMembresia = "Membresía Básica - S/ 81.42 (incl. IGV)";
                    break;
                case "premium":
                    precio = 20886L;
                    nombreMembresia = "Membresía Premium - S/ 208.86 (incl. IGV)";
                    break;
                default:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Plan no válido: " + plan));
            }

            SessionCreateParams.Builder builder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl + "/exito?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/cancel")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("PEN")
                                                    .setUnitAmount(precio)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(nombreMembresia)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    );

            if (dni != null) {
                builder.putMetadata("dni", String.valueOf(dni));
            }
            builder.putMetadata("plan", plan);

            SessionCreateParams params = builder.build();
            Session session = Session.create(params);
            return ResponseEntity.ok(session.getUrl());

        } catch (Exception e) {
            logger.error("Error creando checkout session", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/confirm")
    @Transactional
    public ResponseEntity<?> confirmCheckout(@RequestParam String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);

            if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
                logger.warn("Session {} no está pagada. Estado: {}", sessionId, session.getPaymentStatus());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "La sesión de Pago no está completada"));
            }

            pagoService.guardarPagoDeSesion(session);

            Map<String, String> metadata = session.getMetadata();
            String plan = metadata != null ? metadata.get("plan") : null;
            String dniStr = metadata != null ? metadata.get("dni") : null;
            Long amountTotal = session.getAmountTotal();

            if (dniStr != null) {
                try {
                    Long dni = Long.parseLong(dniStr);

                    if (plan != null) {
                        membresiaService.crearMembresiaPorPlan(dni, plan);
                    } else {
                        Long idMembresia;
                        if (amountTotal != null && (amountTotal == 6990L || amountTotal == 20970L)) {
                            idMembresia = 1L;
                        } else {
                            idMembresia = 2L;
                        }
                        membresiaService.crearMembresia(dni, idMembresia);
                    }

                    pagoService.activarUsuarioPorDni(dni);

                } catch (NumberFormatException nfe) {
                    logger.error("DNI invalido en metadata de sesion {}: '{}'. El pago fue procesado pero el usuario no fue activado.",
                                 sessionId, dniStr);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of(
                                "error", "Error en los datos de la sesion",
                                "detail", "No se pudo identificar al usuario. Contacte a soporte con su ID de sesion: " + sessionId
                            ));
                }
            }

            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            logger.error("Error confirmando checkout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/last-payment")
    public ResponseEntity<?> lastPayment(@RequestParam String dni, HttpServletRequest request) {
        try {
            Long authenticatedDni = getAuthenticatedDni(request);
            if (authenticatedDni == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
            }

            String token = request.getHeader("Authorization").substring(7);
            String rol = jwtUtil.extractRole(token);
            boolean isAdminOrPersonal = "ADMIN".equals(rol) || "PERSONAL".equals(rol);

            if (!isAdminOrPersonal && !authenticatedDni.toString().equals(dni)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo puedes ver tus propios pagos"));
            }

            Pago last = pagoService.obtenerUltimoPagoPorDni(dni);
            if (last != null) return ResponseEntity.ok(last);
            else return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error obteniendo ultimo Pago", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> allPayments() {
        try {
            java.util.List<Pago> pagos = pagoService.obtenerTodosPagos();
            return ResponseEntity.ok(pagos);
        } catch (Exception e) {
            logger.error("Error obteniendo todos los pagos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", e.getMessage()));
        }
    }
}