package com.example.concertticketingsystem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class BusinessConflictException
        extends RuntimeException {

    private final String code;

    public BusinessConflictException(String message) {
        this("RESSOURCE_EXISTANTE", message);
    }

    public BusinessConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
