package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.Document;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.TypeDocument;

import java.time.LocalDateTime;

public record DocumentResponse(
        Long id,
        String nomFichier,
        String contentType,
        Long taille,
        TypeDocument typeDocument,
        Long evenementId,
        String evenementTitre,
        LocalDateTime dateUpload,
        MembreResume deposePar,
        boolean peutArchiver,
        boolean estArchive,
        LocalDateTime dateArchivage,
        MembreResume archivePar) {

    public static DocumentResponse from(Document document) {
        return from(document, false);
    }

    public static DocumentResponse from(Document document, boolean peutArchiver) {
        return new DocumentResponse(
                document.getId(),
                document.getNomFichier(),
                document.getContentType(),
                document.getTaille(),
                document.getTypeDocument(),
                document.getEvenement() == null ? null : document.getEvenement().getId(),
                document.getEvenement() == null ? null : document.getEvenement().getTitre(),
                document.getDateUpload(),
                MembreResume.from(document.getDeposePar()),
                peutArchiver,
                document.isEstArchive(),
                document.getDateArchivage(),
                MembreResume.from(document.getArchivePar()));
    }

    public record MembreResume(Long id, String nom, String prenom) {
        private static MembreResume from(Membre membre) {
            return membre == null ? null : new MembreResume(membre.getId(), membre.getNom(), membre.getPrenom());
        }
    }
}
