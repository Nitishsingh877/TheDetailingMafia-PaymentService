package com.thederailingmafia.carwash.paymentservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
