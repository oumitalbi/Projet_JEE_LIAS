package com.lias.gestion_laboratoire.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank(message = "Le jeton de réinitialisation est obligatoire.")
        String token,
        @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
        @Size(min = 12, max = 128, message = "Le mot de passe doit contenir entre 12 et 128 caractères.")
        String motDePasse,
        @NotBlank(message = "La confirmation du mot de passe est obligatoire.")
        String confirmationMotDePasse
) {
    @AssertTrue(message = "Les deux mots de passe ne correspondent pas.")
    public boolean isConfirmationValide() {
        return motDePasse != null && motDePasse.equals(confirmationMotDePasse);
    }
}
