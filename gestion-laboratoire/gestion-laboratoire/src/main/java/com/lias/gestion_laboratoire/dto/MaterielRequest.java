package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.enums.EtatMateriel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MaterielRequest(
        @NotBlank @Size(max = 200) String nom,
        String description,
        @NotBlank @Size(max = 100) String numeroInventaire,
        @NotBlank @Size(max = 80) String categorie,
        @NotNull EtatMateriel etat,
        LocalDate dateAcquisition,
        @NotNull @Min(1) Integer quantiteInitiale,
        boolean consommable) {
}
