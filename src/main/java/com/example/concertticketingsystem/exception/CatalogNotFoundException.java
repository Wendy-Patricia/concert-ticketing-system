package com.example.concertticketingsystem.exception;

public class CatalogNotFoundException extends RuntimeException {
    private final String code;

    public CatalogNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
