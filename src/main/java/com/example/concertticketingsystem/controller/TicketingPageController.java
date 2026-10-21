
package com.example.concertticketingsystem.controller;

import com.example.concertticketingsystem.dto.request.CustomerRequest;
import com.example.concertticketingsystem.dto.request.TicketPurchaseRequest;
import com.example.concertticketingsystem.service.CustomerService;
import com.example.concertticketingsystem.service.LoyaltyCardService;
import com.example.concertticketingsystem.service.TicketPurchaseService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class TicketingPageController {

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
            model.addAttribute("error", ex.getMessage());
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
            model.addAttribute("error", ex.getMessage());
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
            model.addAttribute("error", ex.getMessage());
        }

        return "purchase-form";
    }
}
