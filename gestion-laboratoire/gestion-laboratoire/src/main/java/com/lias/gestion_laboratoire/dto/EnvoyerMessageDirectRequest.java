package com.lias.gestion_laboratoire.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnvoyerMessageDirectRequest(
        @NotNull(message = "L'identifiant du destinataire est obligatoire")
        Long destinataireId,
        @NotBlank(message = "Le contenu du message ne peut pas être vide")
        @Size(max = 5000, message = "Le message ne peut pas dépasser 5 000 caractères")
        String contenu
) {}
