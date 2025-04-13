package com.thederailingmafia.carwash.paymentservice.service;


import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.thederailingmafia.carwash.paymentservice.dto.OrderResponse;
import com.thederailingmafia.carwash.paymentservice.dto.PaymentRequest;
import com.thederailingmafia.carwash.paymentservice.dto.PaymentResponse;
import com.thederailingmafia.carwash.paymentservice.exception.StripeException;
import com.thederailingmafia.carwash.paymentservice.feign.OrderServiceClient;
import com.thederailingmafia.carwash.paymentservice.model.Payment;
import com.thederailingmafia.carwash.paymentservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



@Service
public class PaymentService {
    @Autowired
    private OrderServiceClient orderServiceClient;

    @Autowired
    private PaymentRepository paymentRepository;

    public PaymentResponse processPayment(PaymentRequest request,String customerEmail) {

        try{
            //validate order
            OrderResponse order = orderServiceClient.getOrderById(request.getOrderId());
            if(!customerEmail.equals(order.getCustomerEmail())) {
                throw new StripeException("Customer email does not match");
            }

            if(!"ACCEPTED".equals(order.getStatus())) {
                throw new StripeException("Order status is not ACCEPTED! , Please wait for a moment");
            }
            if(!request.getAmount().equals(request.getInvoiceAmount())){
                throw new StripeException("Invoice amount does not match");
            }

            //create payment intent
            PaymentIntent paymentIntent = PaymentIntent.create(
                    PaymentIntentCreateParams.builder()
                            .setAmount((long) (request.getAmount()*100))
                            .setCurrency("usd")
                            .setDescription("Payment for Order #" + request.getOrderId())
                            .putMetadata("Order_id",request.getOrderId().toString())
                            .putMetadata("Customer_Email",customerEmail)
                            .addPaymentMethodType("card")
                            .build()
            );

            // Save payment (pending)
            Payment payment = new Payment();
            payment.setOrderId(request.getOrderId());
            payment.setPaymentId(paymentIntent.getId());
            payment.setAmount(request.getAmount());
            payment.setCustomerEmail(customerEmail);
            payment.setStatus("PENDING");
            paymentRepository.save(payment);

            // Return client_secret for browser confirmation
            return new PaymentResponse(
                    paymentIntent.getId(),
                    request.getOrderId(),
                    request.getAmount(),
                    "PENDING",
                    paymentIntent.getClientSecret() // For browser-based confirmation
            );
        } catch (StripeException e) {
            throw new RuntimeException("Payment service error: " + e.getMessage());
        } catch (com.stripe.exception.StripeException e) {
            throw new RuntimeException("Stripe error: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error: " + e.getMessage());
        }
    }


    // Add to PaymentService.java
    public PaymentResponse confirmPayment(String paymentId) {
        try {
            Payment payment = paymentRepository.findByPaymentId(paymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentId);
            if ("succeeded".equals(paymentIntent.getStatus())) {
                payment.setStatus("SUCCESS");
                paymentRepository.save(payment);

                // Update order
                OrderResponse order = orderServiceClient.getOrderById(payment.getOrderId());
                order.setStatus("COMPLETED");
                OrderResponse updatedOrder = orderServiceClient.updateOrder(payment.getOrderId(), order);

                return new PaymentResponse(
                        paymentIntent.getId(),
                        payment.getOrderId(),
                        payment.getAmount(),
                        "SUCCESS",
                        updatedOrder.getStatus()
                );
            } else {
                payment.setStatus("FAILED");
                paymentRepository.save(payment);
                throw new RuntimeException("Payment not confirmed: " + paymentIntent.getStatus());
            }
        } catch (com.stripe.exception.StripeException e) {
            throw new RuntimeException("Stripe error: " + e.getMessage());
        }
    }


}
