package com.example.concertticketingsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @Valid
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "street",
                    column = @Column(name = "street", nullable = false)
            ),
            @AttributeOverride(
                    name = "city",
                    column = @Column(name = "city", nullable = false)
            )
    })
    private Address address;

    public Customer() {}

    public Customer(String name, String email, Address address) {
        this.name = name;
        this.email = email;
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Address getAddress() {
        return address;
    }
}