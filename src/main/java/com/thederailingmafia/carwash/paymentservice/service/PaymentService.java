    package com.thederailingmafia.carwash.paymentservice.service;


    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.stripe.Stripe;
    import com.stripe.model.PaymentIntent;
    import com.stripe.model.checkout.Session;
    import com.stripe.param.checkout.SessionCreateParams;
    import com.thederailingmafia.carwash.paymentservice.dto.PaymentRequest;
    import com.thederailingmafia.carwash.paymentservice.dto.PaymentResponse;
    import com.thederailingmafia.carwash.paymentservice.exception.StripeException;
    import com.thederailingmafia.carwash.paymentservice.feign.OrderServiceClient;
    import com.thederailingmafia.carwash.paymentservice.model.Payment;
    import com.thederailingmafia.carwash.paymentservice.repository.PaymentRepository;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.amqp.rabbit.core.RabbitTemplate;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.security.access.prepost.PreAuthorize;
    import org.springframework.stereotype.Service;

    import java.util.HashMap;
    import java.util.Map;


    @Service
    public class PaymentService {
        @Autowired
        private OrderServiceClient orderServiceClient;

        @Autowired
        private PaymentRepository paymentRepository;
        @Autowired
        private RabbitTemplate rabbitTemplate;

        @Value("${stripe.secret-key}")
        private String secretKey;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @PreAuthorize("hasAnyAuthority('CUSTOMER', 'WASHER')")
        public PaymentResponse getPaymentStatus(Long orderId) {
            try {
                Stripe.apiKey = secretKey;

                // Retrieve the payment record from the database
                Payment payment = paymentRepository.findByOrderId(orderId)
                        .orElseThrow(() -> new RuntimeException("Payment not found for order ID: " + orderId));

                // ✅ First, retrieve the Stripe Checkout session using session ID from DB
                Session session = Session.retrieve(payment.getPaymentId());  // Fetch session from Stripe
                String stripePaymentIntentId = session.getPaymentIntent(); // Extract actual PaymentIntent ID

                // Handle cases where PaymentIntent ID is missing
                if (stripePaymentIntentId == null || stripePaymentIntentId.isEmpty()) {
                    throw new RuntimeException("Stripe session does not contain a valid PaymentIntent for order ID: " + orderId);
                }

                // ✅ Retrieve PaymentIntent from Stripe using the correct ID
                PaymentIntent paymentIntent = PaymentIntent.retrieve(stripePaymentIntentId);

                // Update payment status in DB if changed
                if (!payment.getStatus().equalsIgnoreCase(paymentIntent.getStatus())) {
                    payment.setStatus(paymentIntent.getStatus());
                    paymentRepository.save(payment);
                }

                return new PaymentResponse(
                        paymentIntent.getId(),
                        payment.getOrderId(),
                        payment.getAmount(),
                        payment.getStatus(),
                        session.getSuccessUrl(),
                        session.getUrl()
                );

            } catch (com.stripe.exception.StripeException e) {
                throw new RuntimeException("Stripe error: " + e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException("Unexpected error: " + e.getMessage());
            }
        }


        @PreAuthorize("hasAnyAuthority('CUSTOMER', 'WASHER')")
        public PaymentResponse processPayment(PaymentRequest request, String customerEmail) {
            System.out.println("Processing payment for customer: " + customerEmail);

            try {
                Stripe.apiKey = secretKey;

                SessionCreateParams.LineItem.PriceData.ProductData productData =
                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName("Car Wash Order #" + request.getOrderId())
                                .build();

                SessionCreateParams.LineItem.PriceData priceData =
                        SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount((long) (request.getAmount() * 100))  // Convert to cents
                                .setProductData(productData)
                                .build();

                SessionCreateParams.LineItem lineItem =
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(priceData)
                                .setQuantity(1L)
                                .build();

                SessionCreateParams params =
                        SessionCreateParams.builder()
                                .addLineItem(lineItem)
                                .setMode(SessionCreateParams.Mode.PAYMENT)
                                .setSuccessUrl("http://localhost:5173/payment-success?paymentId={CHECKOUT_SESSION_ID}") // Stripe will replace this
                                .setCancelUrl("http://localhost:5173/payment-failed")
                                .putMetadata("order_id", request.getOrderId().toString())
                                .putMetadata("customer_email", customerEmail)
                                .build();

                Session session = Session.create(params);
                System.out.println("Stripe Checkout Session URL: " + session.getUrl());

                // Save pending payment in DB
                Payment payment = new Payment();
                payment.setOrderId(request.getOrderId());
                payment.setPaymentId(session.getId());
                payment.setAmount(request.getAmount());
                payment.setCustomerEmail(customerEmail);
                payment.setStatus("PENDING");
                payment.setPaymentUrl(session.getUrl());

                paymentRepository.save(payment);

                // Return session URL for redirection
                return new PaymentResponse(
                        session.getId(),
                        request.getOrderId(),
                        request.getAmount(),
                        "PENDING",
                        session.getSuccessUrl(),
                        session.getUrl()  // Stripe hosted checkout page URL
                );

            } catch (StripeException e) {
                throw new RuntimeException("Payment service error: " + e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException("Unexpected error: " + e.getMessage());
            }
        }


        // <----------ConfirmPayment--------->
        @PreAuthorize("hasAnyAuthority('CUSTOMER', 'WASHER')")
        public PaymentResponse confirmPayment(String paymentId) {
            try {
                Stripe.apiKey = secretKey;

                // ✅ Retrieve the actual PaymentIntent ID from Stripe session
                Session session = Session.retrieve(paymentId);
                String stripePaymentIntentId = session.getPaymentIntent();

                if (stripePaymentIntentId == null) {
                    throw new RuntimeException("Stripe session does not contain a PaymentIntent");
                }

                PaymentIntent paymentIntent = PaymentIntent.retrieve(stripePaymentIntentId);

                Payment payment = paymentRepository.findByPaymentId(paymentId)
                        .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

                if ("succeeded".equals(paymentIntent.getStatus())) {
                    payment.setStatus("SUCCESS");
                } else {
                    payment.setStatus("FAILED");
                }

                paymentRepository.save(payment);
                publishPaymentEvent(payment,"payment.confirmed");

                return new PaymentResponse(
                        paymentIntent.getId(),
                        payment.getOrderId(),
                        payment.getAmount(),
                        payment.getStatus(),
                        session.getSuccessUrl(),
                        session.getSuccessUrl()

                );



            } catch (com.stripe.exception.StripeException e) {
                throw new RuntimeException("Stripe error: " + e.getMessage());
            }
        }

        private void publishPaymentEvent(Payment payment, String eventType) {
            try {
                Map<String, Object> event = new HashMap<>();
                event.put("event", eventType);
                event.put("paymentId", payment.getPaymentId());
                event.put("orderId", payment.getOrderId());
                event.put("customerEmail", payment.getCustomerEmail());
                event.put("amount", payment.getAmount());
                event.put("status", payment.getStatus());
                rabbitTemplate.convertAndSend("carwash.events", "notification." + eventType, objectMapper.writeValueAsString(event));
                System.out.println("Published event: " + eventType + " for payment " + payment.getPaymentId());
            } catch (Exception e) {
                System.err.println("Error publishing event " + eventType + ": " + e.getMessage());
            }
        }
    }


