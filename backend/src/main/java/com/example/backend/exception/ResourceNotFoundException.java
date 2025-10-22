package com.example.backend.exception;

/**
 * Eccezione lanciata quando una risorsa non viene trovata
 */
public class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s non trovato con %s: '%s'", resourceName, fieldName, fieldValue));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}