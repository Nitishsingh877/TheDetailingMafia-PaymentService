package com.thederailingmafia.carwash.paymentservice.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "payments")
@Data
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private String paymentId; // Stripe PaymentIntent ID
    private Double amount;
    private String customerEmail;
    private String status;
}
