package com.example.concertticketingsystem.dto.response;

public record CustomerResponse(
        Long id,
        String name,
        String email,
        String street,
        String city
) {}