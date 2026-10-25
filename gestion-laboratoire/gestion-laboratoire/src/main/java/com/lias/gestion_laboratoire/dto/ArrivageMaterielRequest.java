package com.lias.gestion_laboratoire.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ArrivageMaterielRequest(
        @NotNull Long materielId,
        @NotNull @Min(1) Integer quantite,
        @NotNull LocalDate dateArrivage,
        @Size(max = 200) String fournisseur,
        @Size(max = 200) String bonLivraison) {
}
