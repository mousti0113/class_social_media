package com.example.backend.exception;

import com.example.backend.dtos.response.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestore globale delle eccezioni.
 * Intercetta tutte le eccezioni e le trasforma in risposte HTTP standardizzate.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Gestisce ResourceNotFoundException (404)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Risorsa non trovata: {} - Path: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Gestisce ResourceAlreadyExistsException (409)
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceAlreadyExists(
            ResourceAlreadyExistsException ex,
            HttpServletRequest request) {

        log.warn("Conflitto risorsa: {} - Path: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Gestisce InvalidCredentialsException e BadCredentialsException (401)
     */
    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponseDTO> handleInvalidCredentials(
            Exception ex,
            HttpServletRequest request) {

        log.warn("Credenziali non valide - Path: {}", request.getRequestURI());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Username o password non corretti")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Gestisce InvalidTokenException (401)
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidToken(
            InvalidTokenException ex,
            HttpServletRequest request) {

        log.warn("Token non valido: {} - Path: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Gestisce UnauthorizedException (403)
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request) {

        log.warn("Accesso negato: {} - Path: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Gestisce LimitExceededException (400)
     */
    @ExceptionHandler(LimitExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleLimitExceeded(
            LimitExceededException ex,
            HttpServletRequest request) {

        log.warn("Limite superato: {} - Path: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Gestisce InvalidInputException (400)
     */
    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidInput(
            InvalidInputException ex,
            HttpServletRequest request) {

        log.warn("Input non valido: {} - Path: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Gestisce UsernameNotFoundException (404)
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleUsernameNotFound(
            UsernameNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Utente non trovato: {} - Path: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message("Utente non trovato")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Gestisce DisabledException (403)
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponseDTO> handleDisabledException(
            DisabledException ex,
            HttpServletRequest request) {

        log.warn("Account disabilitato - Path: {}", request.getRequestURI());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("Account disabilitato")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Gestisce errori di validazione (400)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        log.warn("Errori di validazione: {} - Path: {}", validationErrors, request.getRequestURI());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message("Errori di validazione nei campi forniti")
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Gestisce tutte le altre eccezioni non previste (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Errore interno del server: {} - Path: {}", ex.getMessage(), request.getRequestURI(), ex);

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Si è verificato un errore interno. Riprova più tardi.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
