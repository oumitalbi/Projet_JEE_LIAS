package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.Document;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.Reunion;
import com.lias.gestion_laboratoire.models.ReunionProcesVerbal;
import com.lias.gestion_laboratoire.models.enums.StatutReunion;

import java.time.LocalDateTime;
import java.util.List;

public record ReunionResponse(
        Long id,
        String titre,
        String ordreDuJour,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        String lieu,
        StatutReunion statut,
        MembreResume organisateur,
        List<MembreResume> participants,
        DocumentResume pv,
        List<PvHistorique> historiquePv,
        boolean archivee,
        MembreResume creePar,
        LocalDateTime dateCreation,
        MembreResume modifiePar,
        LocalDateTime dateModification,
        MembreResume statutChangePar,
        LocalDateTime dateChangementStatut,
        boolean peutModifier,
        boolean peutGererPv,
        boolean peutChangerStatut,
        boolean estParticipant) {

    public static ReunionResponse from(
            Reunion reunion,
            Membre utilisateur,
            boolean peutModifier,
            boolean peutGererPv,
            boolean peutChangerStatut,
            List<ReunionProcesVerbal> historique) {
        List<MembreResume> participants = reunion.getParticipants() == null ? List.of()
                : reunion.getParticipants().stream().map(MembreResume::from).toList();
        boolean estParticipant = reunion.getParticipants() != null && reunion.getParticipants().stream()
                .anyMatch(participant -> utilisateur != null && utilisateur.getId().equals(participant.getId()));
        return new ReunionResponse(
                reunion.getId(),
                reunion.getTitre(),
                reunion.getOrdreDuJour(),
                reunion.getDateReunion(),
                reunion.getDateFin(),
                reunion.getLieu(),
                reunion.getStatut(),
                MembreResume.from(reunion.getOrganisateur()),
                participants,
                DocumentResume.from(reunion.getProcesVerbal()),
                historique == null ? List.of() : historique.stream().map(PvHistorique::from).toList(),
                reunion.isArchivee(),
                MembreResume.from(reunion.getCreePar()),
                reunion.getDateCreation(),
                MembreResume.from(reunion.getModifiePar()),
                reunion.getDateModification(),
                MembreResume.from(reunion.getStatutChangePar()),
                reunion.getDateChangementStatut(),
                peutModifier,
                peutGererPv,
                peutChangerStatut,
                estParticipant);
    }

    public record MembreResume(Long id, String nom, String prenom, String photo, String equipe) {
        private static MembreResume from(Membre membre) {
            return membre == null ? null : new MembreResume(
                    membre.getId(),
                    membre.getNom(),
                    membre.getPrenom(),
                    membre.getPhoto(),
                    membre.getEquipeLaboratoire() == null ? null : membre.getEquipeLaboratoire().name());
        }
    }

    public record DocumentResume(Long id, String nomFichier, String type, LocalDateTime dateDepot) {
        private static DocumentResume from(Document document) {
            return document == null ? null : new DocumentResume(
                    document.getId(),
                    document.getNomFichier(),
                    document.getTypeDocument() == null ? null : document.getTypeDocument().name(),
                    document.getDateUpload());
        }
    }

    public record PvHistorique(Long id, String nomFichier, LocalDateTime dateDepot, MembreResume ajoutePar) {
        private static PvHistorique from(ReunionProcesVerbal historique) {
            Document document = historique.getDocument();
            return new PvHistorique(
                    document.getId(),
                    document.getNomFichier(),
                    historique.getDateAjout(),
                    MembreResume.from(historique.getAjoutePar()));
        }
    }
}
