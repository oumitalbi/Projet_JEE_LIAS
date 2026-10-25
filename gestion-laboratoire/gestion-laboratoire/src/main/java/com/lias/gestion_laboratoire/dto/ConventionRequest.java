package com.lias.gestion_laboratoire.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ConventionRequest {

    @NotBlank(message = "Le titre est obligatoire.")
    @Size(max = 200, message = "Le titre ne peut pas dépasser 200 caractères.")
    private String titre;

    private String description;

    @NotNull(message = "La date de signature est obligatoire.")
    private LocalDate dateSignature;

    @NotNull(message = "La date d'expiration est obligatoire.")
    private LocalDate dateExpiration;

    @NotNull(message = "Le partenaire est obligatoire.")
    private Long partenaireId;

    @AssertTrue(message = "La date d'expiration doit être égale ou postérieure à la date de signature.")
    public boolean isPeriodeValide() {
        return dateSignature == null || dateExpiration == null || !dateExpiration.isBefore(dateSignature);
    }
}
