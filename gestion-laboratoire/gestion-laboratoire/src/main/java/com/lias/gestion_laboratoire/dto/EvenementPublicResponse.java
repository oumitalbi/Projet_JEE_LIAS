package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.Evenement;

import java.time.LocalDateTime;

public record EvenementPublicResponse(
        Long id,
        String titre,
        String description,
        String type,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        String lieu,
        String edition
) {
    public static EvenementPublicResponse from(Evenement evenement) {
        return new EvenementPublicResponse(
                evenement.getId(),
                evenement.getTitre(),
                evenement.getDescription(),
                evenement.getType() == null ? null : evenement.getType().name(),
                evenement.getDateDebut(),
                evenement.getDateFin(),
                evenement.getLieu(),
                evenement.getEdition());
    }
}
