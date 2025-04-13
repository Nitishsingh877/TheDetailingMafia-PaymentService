package com.thederailingmafia.carwash.paymentservice.controller;

import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.thederailingmafia.carwash.paymentservice.dto.PaymentRequest;
import com.thederailingmafia.carwash.paymentservice.dto.PaymentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:8000")
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @PostMapping(value = "/process", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestBody PaymentRequest request,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-User-Email") String userEmail
    ) {
        try {
            if (!userEmail.equals("abc1222@gmail.com")) {
                return ResponseEntity.status(403).body(null);
            }

            PaymentIntent intent = PaymentIntent.create(
                    PaymentIntentCreateParams.builder()
                            .setAmount((long) (request.getAmount() * 100))
                            .setCurrency("usd")
                            .addPaymentMethodType("card")
                            .build()
            );

            jdbcTemplate.update(
                    "INSERT INTO payments (payment_id, order_id, amount, status) VALUES (?, ?, ?, ?)",
                    intent.getId(), request.getOrderId(), request.getAmount(), "PENDING"
            );

            PaymentResponse response = new PaymentResponse();
            response.setPaymentId(intent.getId());
            response.setClientSecret(intent.getClientSecret());
            return ResponseEntity.ok(response);
        } catch (com.stripe.exception.StripeException e) {
            System.err.println("Stripe error: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping(value = "/confirm/{paymentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> confirmPayment(@PathVariable String paymentId) {
        try {
            // Verify payment exists
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM payments WHERE payment_id = ?",
                    Integer.class, paymentId
            );
            if (count == null || count == 0) {
                System.out.println("Payment not found: " + paymentId);
                return ResponseEntity.status(404).body(Map.of("error", "Payment not found"));
            }

            // Update payments
            int paymentRows = jdbcTemplate.update(
                    "UPDATE payments SET status = ? WHERE payment_id = ?",
                    "SUCCESS", paymentId
            );
            System.out.println("Updated payment rows: " + paymentRows);

            // Get order_id
            Integer orderId = jdbcTemplate.queryForObject(
                    "SELECT order_id FROM payments WHERE payment_id = ?",
                    Integer.class, paymentId
            );
            if (orderId == null) {
                System.out.println("Order ID not found for payment: " + paymentId);
                return ResponseEntity.status(400).body(Map.of("error", "Order ID not found"));
            }

            // Verify order exists
            Integer orderCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM car_orders WHERE id = ?",
                    Integer.class, orderId
            );
            if (orderCount == null || orderCount == 0) {
                System.out.println("Order not found: " + orderId);
                return ResponseEntity.status(404).body(Map.of("error", "Order not found"));
            }

            // Update car_orders
            int orderRows = jdbcTemplate.update(
                    "UPDATE car_orders SET status = ? WHERE id = ?",
                    "COMPLETED", orderId
            );
            System.out.println("Updated car_orders rows: " + orderRows);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Payment confirmed");
            response.put("paymentId", paymentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Confirm error for paymentId " + paymentId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to confirm payment: " + e.getMessage()));
        }
    }
}