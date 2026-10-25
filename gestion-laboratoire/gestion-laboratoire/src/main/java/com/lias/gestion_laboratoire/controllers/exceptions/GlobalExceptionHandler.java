package com.lias.gestion_laboratoire.controllers.exceptions;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Gestionnaire d'exceptions global pour standardiser et centraliser
 * le traitement des erreurs sur l'ensemble de l'API REST.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Intercepte les exceptions de ressource introuvable et renvoie un code 404
     * (Not Found).
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleEntityNotFoundException(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * Intercepte les exceptions d'argument invalide (comme l'email déjà existant)
     * et renvoie un code 400 (Bad Request).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    /**
     * Intercepte les ResponseStatusException (comme notre erreur 400 pour l'email)
     * et force l'affichage du message personnalisé au format JSON.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> errorDetails = new LinkedHashMap<>();

        errorDetails.put("timestamp", LocalDateTime.now().toString());
        errorDetails.put("status", ex.getStatusCode().value());
        errorDetails.put("error", HttpStatus.valueOf(ex.getStatusCode().value()).getReasonPhrase());
        errorDetails.put("message", ex.getReason());

        return new ResponseEntity<>(errorDetails, ex.getStatusCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new TreeMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String field = switch (error.getField()) {
                case "dateFinValide" -> "dateFin";
                case "periodeValide" -> "dateExpiration";
                default -> error.getField();
            };
            fields.putIfAbsent(field, error.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "Les données de l’événement sont invalides.", fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fields = new TreeMap<>();
        ex.getConstraintViolations().forEach(violation -> fields.put(
                violation.getPropertyPath().toString(), violation.getMessage()));
        return error(HttpStatus.BAD_REQUEST, "Les données de la requête sont invalides.", fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableRequest(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "Le JSON ou le format d’une date est invalide.", Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "La taille maximale autorisée pour un fichier est de 5 Mo.", Map.of());
    }

    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status,
            String message,
            Map<String, String> fields) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("timestamp", LocalDateTime.now().toString());
        details.put("status", status.value());
        details.put("error", status.getReasonPhrase());
        details.put("message", message);
        details.put("fields", fields);
        return ResponseEntity.status(status).body(details);
    }
}
