package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.*;
import com.lias.gestion_laboratoire.models.enums.Statut;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record ConventionResponse(
        Long id,
        String titre,
        String description,
        LocalDate dateSignature,
        LocalDate dateExpiration,
        String statut,
        boolean archivee,
        PartenaireResume partenaire,
        DocumentResume document,
        List<DocumentResume> historiqueDocuments,
        List<ActiviteResume> activites,
        MembreResume creePar,
        LocalDateTime dateCreation,
        MembreResume modifiePar,
        LocalDateTime dateModification,
        MembreResume archivePar,
        LocalDateTime dateArchivage,
        MembreResume restaurePar,
        LocalDateTime dateRestauration,
        boolean peutModifier,
        boolean peutArchiver) {

    public static ConventionResponse from(
            Convention convention,
            boolean peutModifier,
            List<ConventionDocumentHistorique> historique) {
        Set<Evenement> events = convention.getActivites();
        return new ConventionResponse(
                convention.getId(),
                convention.getTitre(),
                convention.getDescription(),
                convention.getDateSignature(),
                convention.getDateExpiration(),
                statut(convention),
                convention.isArchivee(),
                PartenaireResume.from(convention.getPartenaire()),
                DocumentResume.from(convention.getDocumentConvention()),
                historique == null ? List.of() : historique.stream()
                        .map(item -> DocumentResume.from(item.getDocument(), item.getDateAjout()))
                        .toList(),
                events == null ? List.of() : events.stream().map(ActiviteResume::from).toList(),
                MembreResume.from(convention.getCreePar()),
                convention.getDateCreation(),
                MembreResume.from(convention.getModifiePar()),
                convention.getDateModification(),
                MembreResume.from(convention.getArchivePar()),
                convention.getDateArchivage(),
                MembreResume.from(convention.getRestaurePar()),
                convention.getDateRestauration(),
                peutModifier && !convention.isArchivee(),
                peutModifier);
    }

    private static String statut(Convention convention) {
        if (convention.isArchivee()) return "ARCHIVEE";
        if (convention.getDateSignature() == null || convention.getDateExpiration() == null) return "BROUILLON";
        LocalDate today = LocalDate.now();
        if (today.isBefore(convention.getDateSignature())) return "A_VENIR";
        return convention.getDateExpiration().isBefore(today) ? "EXPIREE" : "ACTIVE";
    }

    public record PartenaireResume(Long id, String nom, String type, String pays, String contactEmail) {
        private static PartenaireResume from(Partenaire partenaire) {
            return partenaire == null ? null : new PartenaireResume(partenaire.getId(), partenaire.getNom(),
                    partenaire.getType() == null ? null : partenaire.getType().name(),
                    partenaire.getPays(), partenaire.getContactEmail());
        }
    }

    public record DocumentResume(Long id, String nomFichier, String type, LocalDateTime dateDepot) {
        private static DocumentResume from(Document document) {
            return document == null ? null : from(document, document.getDateUpload());
        }

        private static DocumentResume from(Document document, LocalDateTime dateDepot) {
            return document == null ? null : new DocumentResume(document.getId(), document.getNomFichier(),
                    document.getTypeDocument() == null ? null : document.getTypeDocument().name(), dateDepot);
        }
    }

    public record ActiviteResume(Long id, String titre, String type, String statut,
                                 LocalDateTime dateDebut, LocalDateTime dateFin) {
        private static ActiviteResume from(Evenement event) {
            return new ActiviteResume(event.getId(), event.getTitre(),
                    event.getType() == null ? null : event.getType().name(),
                    event.getStatut() == null ? null : event.getStatut().name(),
                    event.getDateDebut(), event.getDateFin());
        }
    }

    public record MembreResume(Long id, String nom, String prenom) {
        private static MembreResume from(Membre membre) {
            return membre == null ? null : new MembreResume(membre.getId(), membre.getNom(), membre.getPrenom());
        }
    }
}
