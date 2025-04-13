package com.thederailingmafia.carwash.paymentservice.feign;

import com.thederailingmafia.carwash.paymentservice.config.FeignClientConfig;
import com.thederailingmafia.carwash.paymentservice.dto.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "booking-service", url = "http://localhost:8084",configuration = FeignClientConfig.class)
public interface OrderServiceClient {
    @GetMapping("/api/order/{id}")
    OrderResponse getOrderById(@PathVariable("id") Long id);

    @PutMapping("/api/order/{id}")
    OrderResponse updateOrder(@PathVariable("id") Long id, @RequestBody OrderResponse order);
}
