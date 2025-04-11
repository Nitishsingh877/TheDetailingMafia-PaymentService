package com.thederailingmafia.carwash.paymentservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class UserModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String password;
    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    private UserRole userRole;
    private LocalDateTime timeStamp;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private Customer customer;

    private String address;
    private String phoneNumber;
    private String auth;

    public UserModel(String name, String password,String email, UserRole userRole, String address, String phoneNumber,String auth) {
        this.name = name;
        this.password = password;
        this.email = email;
        this.userRole = userRole;
        this.timeStamp = LocalDateTime.now();
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.auth = auth;
    }
}


