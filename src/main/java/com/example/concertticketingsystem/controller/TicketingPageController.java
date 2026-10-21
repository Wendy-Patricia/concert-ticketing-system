
package com.example.concertticketingsystem.controller;

import com.example.concertticketingsystem.dto.request.CustomerRequest;
import com.example.concertticketingsystem.dto.request.TicketPurchaseRequest;
import com.example.concertticketingsystem.service.CustomerService;
import com.example.concertticketingsystem.service.LoyaltyCardService;
import com.example.concertticketingsystem.service.TicketPurchaseService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.concertticketingsystem.exception.BusinessConflictException;
import com.example.concertticketingsystem.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;

@Controller
public class TicketingPageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TicketingPageController.class);

    private final CustomerService customerService;
    private final LoyaltyCardService loyaltyCardService;
    private final TicketPurchaseService ticketPurchaseService;

    public TicketingPageController(
            CustomerService customerService,
            LoyaltyCardService loyaltyCardService,
            TicketPurchaseService ticketPurchaseService) {

        this.customerService = customerService;
        this.loyaltyCardService = loyaltyCardService;
        this.ticketPurchaseService = ticketPurchaseService;
    }

    @GetMapping("/clients/inscription")
    public String customerForm() {
        return "customer-form";
    }

    @PostMapping("/clients/inscription")
    public String registerCustomer(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String street,
            @RequestParam String city,
            Model model) {

        try {
            var result = customerService.createCustomer(
                    new CustomerRequest(name, email, street, city)
            );

            model.addAttribute("message",
                    "Client créé avec succès ! ID : " + result.id());

        } catch (RuntimeException ex) {
            displayError(model, ex);
        }

        return "customer-form";
    }

    @GetMapping("/fidelite")
    public String loyaltyForm() {
        return "loyalty-form";
    }

    @PostMapping("/fidelite")
    public String purchaseLoyaltyCard(
            @RequestParam Long customerId,
            Model model) {

        try {
            var result = loyaltyCardService.purchaseCard(customerId);

            model.addAttribute("message",
                    "Carte créée ! Valable jusqu'au "
                            + result.expiresAt());

        } catch (RuntimeException ex) {
            displayError(model, ex);
        }

        return "loyalty-form";
    }

    @GetMapping("/billets/acheter")
    public String purchaseForm() {
        return "purchase-form";
    }

    @PostMapping("/billets/acheter")
    public String purchaseTickets(
            @RequestParam Long concertId,
            @RequestParam Long customerId,
            @RequestParam Integer quantity,
            Model model) {

        try {
            var result = ticketPurchaseService.purchase(
                    new TicketPurchaseRequest(
                            concertId, customerId, quantity
                    )
            );

            model.addAttribute("message",
                    "Achat confirmé ! Code : " + result.code()
                            + " | Total : " + result.totalAmount() + " €");

        } catch (RuntimeException ex) {
            displayError(model, ex);
        }

        return "purchase-form";
    }

    private void displayError(Model model, RuntimeException exception) {
        if (exception instanceof BusinessConflictException || exception instanceof ResourceNotFoundException
                || exception instanceof IllegalArgumentException || exception instanceof ConstraintViolationException) {
            model.addAttribute("error", exception.getMessage());
        } else {
            LOGGER.error("Echec d'une operation de billetterie", exception);
            model.addAttribute("error", "Opération impossible. Veuillez réessayer.");
        }
    }
}
