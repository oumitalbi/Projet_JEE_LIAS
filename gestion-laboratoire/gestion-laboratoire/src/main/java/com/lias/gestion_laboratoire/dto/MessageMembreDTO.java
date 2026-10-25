package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.Statut;

/**
 * DTO résumé d'un membre pour les messages (évite les boucles Jackson et masque le mot de passe).
 */
public record MessageMembreDTO(
        Long id,
        String nom,
        String prenom,
        String email,
        String photo,
        Role role,
        Statut statut
) {
    public static MessageMembreDTO from(Membre m) {
        if (m == null) return null;
        return new MessageMembreDTO(
                m.getId(),
                m.getNom(),
                m.getPrenom(),
                m.getEmail(),
                m.getPhoto(),
                m.getRole(),
                m.getStatut()
        );
    }
}
