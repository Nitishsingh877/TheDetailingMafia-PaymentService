package com.thederailingmafia.carwash.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String paymentId;
    private Long orderId;
    private Double amount;
    private String status;
    private String orderStatus;
    private String clientSecret;

    public PaymentResponse(String paymentId, Long orderId, Double amount, String orderStatus, String clientSecret) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.orderStatus = orderStatus;
        this.clientSecret = clientSecret;
    }
}
