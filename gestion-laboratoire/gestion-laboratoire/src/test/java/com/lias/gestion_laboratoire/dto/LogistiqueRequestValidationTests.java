package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.enums.EtatMateriel;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogistiqueRequestValidationTests {
    private static jakarta.validation.ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void requiresPositiveQuantityAndJustificationOnDemand() {
        assertFalse(validator.validate(new DemandeMaterielRequest(1L, null, 0, " ")).isEmpty());
        assertTrue(validator.validate(new DemandeMaterielRequest(1L, null, 2, "Besoin professionnel")).isEmpty());
    }

    @Test
    void requiresCompleteInventoryAndPositiveInitialQuantity() {
        assertFalse(validator.validate(new MaterielRequest("", null, "", "", EtatMateriel.NEUF,
                null, 0, false)).isEmpty());
        assertTrue(validator.validate(new MaterielRequest("Ordinateur", null, "LIAS-1", "Informatique",
                EtatMateriel.NEUF, null, 1, false)).isEmpty());
    }

    @Test
    void refusalReasonCannotBeBlank() {
        assertFalse(validator.validate(new RefusDemandeRequest("  ")).isEmpty());
        assertTrue(validator.validate(new RefusDemandeRequest("Priorité accordée à une autre demande")).isEmpty());
    }
}
