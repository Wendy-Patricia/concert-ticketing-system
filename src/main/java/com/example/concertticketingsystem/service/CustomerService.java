package com.example.concertticketingsystem.service;

import com.example.concertticketingsystem.dto.request.CustomerRequest;
import com.example.concertticketingsystem.dto.response.CustomerResponse;
import com.example.concertticketingsystem.entity.Address;
import com.example.concertticketingsystem.entity.Customer;
import com.example.concertticketingsystem.exception.BusinessConflictException;
import com.example.concertticketingsystem.repository.CustomerRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

@Service
@Validated
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse createCustomer(@Valid CustomerRequest request) {

        if (customerRepository.existsByEmail(request.email())) {
            throw new BusinessConflictException(
                    "Un client avec cet email existe déjà"
            );
        }

        Address address = new Address(
                request.street(),
                request.city()
        );

        Customer customer = new Customer(
                request.name(),
                request.email(),
                address
        );

        try {
            Customer saved = customerRepository.saveAndFlush(customer);

            return new CustomerResponse(
                    saved.getId(),
                    saved.getName(),
                    saved.getEmail(),
                    saved.getAddress().getStreet(),
                    saved.getAddress().getCity()
            );
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessConflictException(
                    "Impossible de créer le client : données déjà utilisées"
            );
        }
    }
}
