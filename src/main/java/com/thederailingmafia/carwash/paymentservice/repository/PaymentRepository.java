package com.thederailingmafia.carwash.paymentservice.repository;

import com.thederailingmafia.carwash.paymentservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentId(String paymentId);
}
