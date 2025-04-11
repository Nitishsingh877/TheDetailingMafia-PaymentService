package com.thederailingmafia.carwash.paymentservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false,unique = true)
    @JsonBackReference
    private UserModel user;

    private String name;
    private String address;
    private String phoneNumber;
    private String email;
    private String password;

    @OneToMany(mappedBy = "customer",cascade = CascadeType.ALL,orphanRemoval = true)
    @JsonManagedReference
    private List<Car> carList = new ArrayList<>();

    public Customer(String name, String address, String phoneNumber,String email) {
        this.name = name;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }
}



