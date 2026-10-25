package com.lias.gestion_laboratoire.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EvenementAnnulationRequest(
        @NotBlank(message = "Le motif d'annulation est obligatoire.")
        @Size(max = 2000, message = "Le motif ne peut pas dépasser 2000 caractères.")
        String motif
) {
}
