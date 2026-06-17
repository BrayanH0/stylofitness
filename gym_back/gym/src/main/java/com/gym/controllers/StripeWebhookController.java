package com.gym.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.services.PagoService;
import com.gym.services.MembresiaUsuarioService;
import com.gym.models.MembresiaUsuario;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gym.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

    @Autowired
    private MembresiaUsuarioService membresiaService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final PagoService pagoService;

    public StripeWebhookController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader,
            @RequestBody String payload) {

        if (sigHeader == null || sigHeader.isBlank()) {
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(payload);
            JsonNode data = json.path("data").path("object");

            switch (event.getType()) {

                case "checkout.session.completed":
                    String paymentStatus = data.path("payment_status").asText("unknown");
                    if (!"paid".equalsIgnoreCase(paymentStatus)) {
                        logger.warn("checkout.session.completed con payment_status={}. Se ignora activación.", paymentStatus);
                        break;
                    }

                    String sessionId = data.path("id").asText("no-session");
                    String paymentIntentId = data.path("payment_intent").asText("no-pi");
                    String email = data.path("customer_details").path("email").asText("no-email");
                    int amountTotal = data.path("amount_total").asInt(0);

                    String planStr = data.path("metadata").path("plan").asText(null);
                    String dniStrForPago = data.path("metadata").path("dni").asText(null);

                    if (planStr != null) {
                        if (dniStrForPago != null) {
                            pagoService.guardarPagoManual(sessionId, paymentIntentId, amountTotal, email, planStr, dniStrForPago);
                        } else {
                            pagoService.guardarPagoManual(sessionId, paymentIntentId, amountTotal, email, planStr);
                        }
                    } else {
                        if (dniStrForPago != null) {
                            pagoService.guardarPagoManual(sessionId, paymentIntentId, amountTotal, email, null, dniStrForPago);
                        } else {
                            pagoService.guardarPagoManual(sessionId, paymentIntentId, amountTotal, email);
                        }
                    }

                    try {
                        String dniStr = data.path("metadata").path("dni").asText(null);

                        if (dniStr != null) {
                            Long dni;
                            try {
                                dni = Long.parseLong(dniStr);
                            } catch (NumberFormatException nfe) {
                                logger.error("DNI invalido en webhook para session {}: '{}'", sessionId, dniStr);
                                break;
                            }

                            if (planStr != null) {
                                membresiaService.crearMembresiaPorPlan(dni, planStr);
                            } else {
                                Long idMembresia;
                                if (amountTotal == 6990 || amountTotal == 20970) {
                                    idMembresia = 1L;
                                } else {
                                    idMembresia = 2L;
                                }
                                membresiaService.crearMembresia(dni, idMembresia);
                            }

                            pagoService.activarUsuarioPorDni(dni);
                        }

                    } catch (Exception e) {
                        logger.error("Error procesando membresia/activacion en webhook: {}", e.getMessage(), e);
                    }

                    break;

                case "payment_intent.succeeded":
                    String piId = data.path("id").asText("no-pi");
                    String piEmail = data.path("receipt_email").asText("no-email");
                    int piAmount = data.path("amount_received").asInt(0);
                    String piCurrency = data.path("currency").asText("USD");
                    String piStatus = data.path("status").asText("unknown");

                    pagoService.guardarPagoDePaymentIntentFromWebhook(piId, piAmount, piCurrency, piEmail, piStatus);
                    break;

                default:
                    break;
            }

        } catch (Exception e) {
            logger.error("Error procesando webhook de Stripe: {}", e.getMessage());
        }

        return ResponseEntity.ok("received");
    }

    private Long getAuthenticatedDni(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.extractDni(token);
        }
        return null;
    }

    @GetMapping("/membresia-activa/{dni}")
    public ResponseEntity<?> obtenerMembresiActiva(@PathVariable Long dni, HttpServletRequest request) {
        try {
            Long authenticatedDni = getAuthenticatedDni(request);
            if (authenticatedDni == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
            }

            String token = request.getHeader("Authorization").substring(7);
            String rol = jwtUtil.extractRole(token);
            boolean isAdminOrPersonal = "ADMIN".equals(rol) || "PERSONAL".equals(rol);

            if (!isAdminOrPersonal && !authenticatedDni.equals(dni)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo puedes ver tu propia membresia"));
            }

            MembresiaUsuario membresia = membresiaService.obtenerMembresiActivaDelUsuario(dni);
            if (membresia != null) {
                return ResponseEntity.ok(membresia);
            } else {
                return ResponseEntity.noContent().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}