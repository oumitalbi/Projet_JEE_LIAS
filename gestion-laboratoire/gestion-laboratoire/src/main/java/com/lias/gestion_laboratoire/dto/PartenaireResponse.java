package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.Partenaire;

import java.time.LocalDateTime;

public record PartenaireResponse(
        Long id,
        String nom,
        String type,
        String pays,
        String contactEmail,
        boolean archive,
        String archivePar,
        LocalDateTime dateArchivage,
        String restaurePar,
        LocalDateTime dateRestauration) {

    public static PartenaireResponse from(Partenaire partenaire) {
        Membre archiviste = partenaire.getArchivePar();
        Membre restaurateur = partenaire.getRestaurePar();
        return new PartenaireResponse(
                partenaire.getId(),
                partenaire.getNom(),
                partenaire.getType() == null ? null : partenaire.getType().name(),
                partenaire.getPays(),
                partenaire.getContactEmail(),
                partenaire.isArchive(),
                archiviste == null ? null : archiviste.getPrenom() + " " + archiviste.getNom(),
                partenaire.getDateArchivage(),
                restaurateur == null ? null : restaurateur.getPrenom() + " " + restaurateur.getNom(),
                partenaire.getDateRestauration());
    }
}
