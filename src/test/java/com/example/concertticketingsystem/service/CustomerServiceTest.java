
package com.example.concertticketingsystem.service;

import com.example.concertticketingsystem.dto.request.CustomerRequest;
import com.example.concertticketingsystem.dto.response.CustomerResponse;
import com.example.concertticketingsystem.entity.Customer;
import com.example.concertticketingsystem.exception.BusinessConflictException;
import com.example.concertticketingsystem.repository.CustomerRepository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerServiceTest {

    private final CustomerRepository repository =
            mock(CustomerRepository.class);

    private final CustomerService service =
            new CustomerService(repository);

    @Test
    void shouldCreateCustomer() {

        CustomerRequest request = new CustomerRequest(
                "Alice",
                "alice@example.com",
                "Rue de Paris",
                "Nancy"
        );

        when(repository.existsByEmail("alice@example.com"))
                .thenReturn(false);

        when(repository.saveAndFlush(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response =
                service.createCustomer(request);

        assertEquals("Alice", response.name());
        assertEquals("alice@example.com", response.email());
        assertEquals("Nancy", response.city());

        verify(repository).saveAndFlush(any(Customer.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {

        CustomerRequest request = new CustomerRequest(
                "Alice",
                "alice@example.com",
                "Rue de Paris",
                "Nancy"
        );

        when(repository.existsByEmail("alice@example.com"))
                .thenReturn(true);

        assertThrows(
                BusinessConflictException.class,
                () -> service.createCustomer(request)
        );

        verify(repository, never())
                .saveAndFlush(any(Customer.class));
    }
}
