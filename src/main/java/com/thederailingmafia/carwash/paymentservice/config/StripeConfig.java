package com.thederailingmafia.carwash.paymentservice.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {
    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        System.out.println("Setting Stripe API Key: " + stripeApiKey); // Debug
        Stripe.apiKey = stripeApiKey;
    }
}
