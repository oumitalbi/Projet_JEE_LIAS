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

class ReunionRequestValidationTests {

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
    void rejectsMeetingWithoutParticipants() {
        ReunionRequest request = validRequest();
        request.setParticipantsIds(Set.of());

        assertTrue(validator.validate(request).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("participantsIds")));
    }

    @Test
    void rejectsMeetingWithoutAgenda() {
        ReunionRequest request = validRequest();
        request.setOrdreDuJour(" ");

        assertTrue(validator.validate(request).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("ordreDuJour")));
    }

    @Test
    void rejectsMeetingWithoutDates() {
        ReunionRequest request = validRequest();
        request.setDateDebut(null);
        request.setDateFin(null);

        var errors = validator.validate(request);
        assertTrue(errors.stream().anyMatch(error -> error.getPropertyPath().toString().equals("dateDebut")));
        assertTrue(errors.stream().anyMatch(error -> error.getPropertyPath().toString().equals("dateFin")));
    }

    @Test
    void rejectsMeetingWhenEndPrecedesStart() {
        ReunionRequest request = validRequest();
        request.setDateFin(request.getDateDebut().minusMinutes(1));

        assertTrue(validator.validate(request).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("dateFinValide")));
    }

    private ReunionRequest validRequest() {
        ReunionRequest request = new ReunionRequest();
        request.setTitre("Conseil de laboratoire");
        request.setOrdreDuJour("Bilan et calendrier");
        request.setDateDebut(LocalDateTime.now().plusDays(1));
        request.setDateFin(LocalDateTime.now().plusDays(1).plusHours(1));
        request.setLieu("Salle LIAS");
        request.setParticipantsIds(Set.of(1L));
        return request;
    }
}
