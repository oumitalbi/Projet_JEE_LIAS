package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.dtos.DemandeCreateRequest;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.Statut;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemandeCreateRequestValidationTests {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requiresConsentAndValidApplicationFields() {
        DemandeCreateRequest request = new DemandeCreateRequest();
        request.setNom("Alaoui");
        request.setPrenom("Salma");
        request.setEmail("salma@example.invalid");
        request.setStatutSouhaite(Statut.PERMANENT);
        request.setEtablissementOrigine("Université Hassan II");
        request.setEquipeSouhaitee(EquipeLaboratoire.ISDIAC);
        request.setLettreMotivation("Motivation sufficiently long to meet the minimum length validation.");
        request.setCv(new MockMultipartFile("cv", "cv.pdf", "application/pdf", "%PDF-1.7".getBytes()));

        assertFalse(validator.validate(request).isEmpty());

        request.setConsentementDonnees(true);
        assertTrue(validator.validate(request).isEmpty());
    }
}
