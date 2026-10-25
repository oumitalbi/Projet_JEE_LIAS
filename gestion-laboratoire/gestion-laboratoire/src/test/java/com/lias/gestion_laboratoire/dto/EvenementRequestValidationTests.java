package com.lias.gestion_laboratoire.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EvenementRequestValidationTests {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void rejectsRequestWithoutOrganizer() {
        EvenementRequest request = validRequest();
        request.setOrganisateursIds(Set.of());

        assertTrue(validator.validate(request).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("organisateursIds")));
    }

    @Test
    void rejectsRequestWithoutDates() {
        EvenementRequest request = validRequest();
        request.setDateDebut(null);
        request.setDateFin(null);

        var errors = validator.validate(request);
        assertTrue(errors.stream().anyMatch(error -> error.getPropertyPath().toString().equals("dateDebut")));
        assertTrue(errors.stream().anyMatch(error -> error.getPropertyPath().toString().equals("dateFin")));
    }

    @Test
    void rejectsRequestWhenEndPrecedesStart() {
        EvenementRequest request = validRequest();
        request.setDateFin(request.getDateDebut().minusMinutes(1));

        assertTrue(validator.validate(request).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("dateFinValide")));
    }

    private EvenementRequest validRequest() {
        EvenementRequest request = new EvenementRequest();
        request.setTitre("Séminaire de test");
        request.setType(com.lias.gestion_laboratoire.models.enums.TypeEvenement.SEMINAIRE);
        request.setDateDebut(LocalDateTime.now().plusDays(1));
        request.setDateFin(LocalDateTime.now().plusDays(1).plusHours(1));
        request.setLieu("LIAS");
        request.setOrganisateursIds(Set.of(1L));
        return request;
    }
}
