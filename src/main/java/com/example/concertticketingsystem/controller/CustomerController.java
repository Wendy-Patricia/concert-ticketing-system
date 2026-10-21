package com.example.concertticketingsystem.controller;

import com.example.concertticketingsystem.dto.request.CustomerRequest;
import com.example.concertticketingsystem.dto.response.CustomerResponse;
import com.example.concertticketingsystem.service.CustomerService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerRequest request) {

        CustomerResponse response =
                customerService.createCustomer(request);

        return ResponseEntity
                .created(URI.create("/api/clients/" + response.id()))
                .body(response);
    }
}
