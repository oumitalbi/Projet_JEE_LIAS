package com.lias.gestion_laboratoire.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConventionRequestValidationTests {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void rejectsMissingRequiredConventionFields() {
        ConventionRequest request = new ConventionRequest();
        assertTrue(validator.validate(request).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("titre")));
        assertTrue(validator.validate(request).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("partenaireId")));
        assertTrue(validator.validate(request).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("dateSignature")));
        assertTrue(validator.validate(request).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("dateExpiration")));
    }

    @Test
    void rejectsExpirationBeforeSignature() {
        ConventionRequest request = new ConventionRequest();
        request.setTitre("Accord LIAS");
        request.setPartenaireId(1L);
        request.setDateSignature(LocalDate.of(2026, 5, 1));
        request.setDateExpiration(LocalDate.of(2026, 4, 30));

        assertTrue(validator.validate(request).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("periodeValide")));
    }
}
