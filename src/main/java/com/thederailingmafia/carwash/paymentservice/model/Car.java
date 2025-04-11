package com.thederailingmafia.carwash.paymentservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "cars")
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long carId;

    private String userEmail;
    private String brand;
    private String model;
    private String licenseNumberPlate;

    public Car(String brand, String model, String licenseNumberPlate) {
        this.brand = brand;
        this.model = model;
        this.licenseNumberPlate = licenseNumberPlate;
    }

    @ManyToOne
    @JoinColumn(name = "customer_id",nullable = false)
    @JsonBackReference
    private Customer customer;

}



