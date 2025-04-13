package com.thederailingmafia.carwash.paymentservice.controller;

import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.thederailingmafia.carwash.paymentservice.dto.PaymentRequest;
import com.thederailingmafia.carwash.paymentservice.dto.PaymentResponse;
import com.thederailingmafia.carwash.paymentservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:8000")
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PaymentService paymentService;

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @PostMapping(value = "/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestBody PaymentRequest request,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("Customer-Email") String customerEmail,
            Authentication authentication
    ) {
        try {
            System.out.println("Received payment request for customer: " + customerEmail + ", auth: " + authentication.getName());

            if(!authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_WASHER"))){
                System.err.println("Unauthorized: Requester is not a WASHER");
                return ResponseEntity.status(403).body(null);
            }
            PaymentResponse response = paymentService.processPayment(request, customerEmail);
            return ResponseEntity.ok(response);
        }catch (Exception e) {
            System.err.println("Failed to process payment: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping(value = "/confirm/{paymentId}")
    public ResponseEntity<Map<String, String>> confirmPayment(@PathVariable String paymentId) {
        try {
            PaymentResponse response = paymentService.confirmPayment(paymentId);
            Map<String, String> result = new HashMap<>();
            result.put("message", "Payment confirmed");
            result.put("paymentId", response.getPaymentId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Confirm error for paymentId " + paymentId + ": " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to confirm payment: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}