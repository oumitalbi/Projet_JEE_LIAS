package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.enums.TypePartenaire;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PartenaireRequest(
        @NotBlank(message = "Le nom du partenaire est obligatoire.")
        @Size(max = 200, message = "Le nom ne peut pas dépasser 200 caractères.")
        String nom,
        @NotNull(message = "Le type du partenaire est obligatoire.")
        TypePartenaire type,
        @Size(max = 120, message = "Le pays ne peut pas dépasser 120 caractères.")
        String pays,
        @Email(message = "L'adresse e-mail du contact est invalide.")
        @Size(max = 255, message = "L'adresse e-mail est trop longue.")
        String contactEmail) {
}
