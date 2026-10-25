package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnvoyerMessageEquipeRequest(
        @NotNull(message = "L'équipe destinataire est obligatoire")
        EquipeLaboratoire equipe,
        @NotBlank(message = "Le contenu du message ne peut pas être vide")
        @Size(max = 5000, message = "Le message ne peut pas dépasser 5 000 caractères")
        String contenu
) {}
