package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.ArrivageMateriel;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ArrivageMaterielResponse(
        Long id, Long materielId, String materielNom, Integer quantite,
        LocalDate dateArrivage, String fournisseur, String bonLivraison,
        String enregistrePar, LocalDateTime dateCreation) {
    public static ArrivageMaterielResponse from(ArrivageMateriel arrival) {
        return new ArrivageMaterielResponse(arrival.getId(), arrival.getMateriel().getId(),
                arrival.getMateriel().getNom(), arrival.getQuantite(), arrival.getDateArrivage(),
                arrival.getFournisseur(), arrival.getBonLivraison(),
                arrival.getEnregistrePar().getPrenom() + " " + arrival.getEnregistrePar().getNom(),
                arrival.getDateCreation());
    }
}
