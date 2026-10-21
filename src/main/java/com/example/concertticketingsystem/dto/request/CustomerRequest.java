package com.example.concertticketingsystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String street,
        @NotBlank String city
) {}