package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.Materiel;

import java.time.LocalDate;

public record MaterielResponse(
        Long id,
        String nom,
        String description,
        String numeroInventaire,
        String categorie,
        String etat,
        LocalDate dateAcquisition,
        Integer quantiteTotale,
        Integer quantiteDisponible,
        boolean consommable,
        boolean archivee,
        boolean disponible,
        String creePar,
        java.time.LocalDateTime dateCreation,
        String modifiePar,
        java.time.LocalDateTime dateModification,
        String archivePar,
        java.time.LocalDateTime dateArchivage,
        String restaurePar,
        java.time.LocalDateTime dateRestauration) {

    public static MaterielResponse from(Materiel item) {
        return new MaterielResponse(item.getId(), item.getNom(), item.getDescription(),
                item.getNumeroInventaire(), item.getCategorie(),
                item.getEtat() == null ? null : item.getEtat().name(),
                item.getDateAcquisition(), item.getQuantiteTotale(), item.getQuantiteDisponible(),
                item.isConsommable(), item.isArchivee(),
                !item.isArchivee() && item.getQuantiteDisponible() != null && item.getQuantiteDisponible() > 0,
                name(item.getCreePar()), item.getDateCreation(), name(item.getModifiePar()),
                item.getDateModification(), name(item.getArchivePar()), item.getDateArchivage(),
                name(item.getRestaurePar()), item.getDateRestauration());
    }

    private static String name(com.lias.gestion_laboratoire.models.Membre member) {
        return member == null ? null : member.getPrenom() + " " + member.getNom();
    }
}
