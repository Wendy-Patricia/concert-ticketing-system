package com.example.concertticketingsystem.controller;

import java.time.Clock;
import java.util.stream.Collectors;

import com.example.concertticketingsystem.dto.ApiError;
import com.example.concertticketingsystem.exception.CatalogConflictException;
import com.example.concertticketingsystem.exception.CatalogNotFoundException;
import com.example.concertticketingsystem.exception.CatalogValidationException;
import com.example.concertticketingsystem.exception.BusinessConflictException;
import com.example.concertticketingsystem.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = {ArtistController.class, VenueController.class, ConcertController.class,
        CustomerController.class, LoyaltyCardController.class, TicketPurchaseController.class})
public class CatalogExceptionHandler {
    private final Clock clock;

    public CatalogExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> ticketingNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getCode(), exception.getMessage(), request);
    }

    @ExceptionHandler(BusinessConflictException.class)
    public ResponseEntity<ApiError> ticketingConflict(BusinessConflictException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> invalidTicketingInput(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ECHOUEE", "Données de billetterie invalides.", request);
    }

    @ExceptionHandler(CatalogNotFoundException.class)
    public ResponseEntity<ApiError> notFound(CatalogNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getCode(), exception.getMessage(), request);
    }

    @ExceptionHandler(CatalogConflictException.class)
    public ResponseEntity<ApiError> conflict(CatalogConflictException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "RESSOURCE_EXISTANTE", exception.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrityConflict(HttpServletRequest request) {
        // Couvre aussi une creation concurrente survenue apres le controle d'unicite.
        return error(HttpStatus.CONFLICT, "CONFLIT_INTEGRITE",
                "L'opération entre en conflit avec les contraintes du catalogue.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException exception,
                                               HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .distinct().sorted().collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ECHOUEE", message, request);
    }

    @ExceptionHandler(CatalogValidationException.class)
    public ResponseEntity<ApiError> invalidRange(CatalogValidationException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ECHOUEE", exception.getMessage(), request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiError> invalidInput(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ECHOUEE",
                "Corps JSON ou paramètres de requête invalides.", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                            HttpServletRequest request) {
        return ResponseEntity.status(status).body(
                new ApiError(clock.instant(), status.value(), code, message, request.getRequestURI()));
    }
}
