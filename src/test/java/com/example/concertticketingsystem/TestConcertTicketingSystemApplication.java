package com.example.concertticketingsystem;

import org.springframework.boot.SpringApplication;

public class TestConcertTicketingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.from(ConcertTicketingSystemApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
