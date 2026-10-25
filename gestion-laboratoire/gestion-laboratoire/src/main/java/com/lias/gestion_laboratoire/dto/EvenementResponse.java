package com.lias.gestion_laboratoire.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lias.gestion_laboratoire.models.Evenement;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.StatutEvenement;
import com.lias.gestion_laboratoire.models.enums.TypeEvenement;

import java.time.LocalDateTime;
import java.util.List;

public record EvenementResponse(
        Long id,
        String titre,
        String description,
        TypeEvenement type,
        StatutEvenement statut,
        String edition,
        Integer annee,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dateDebut,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dateFin,
        String lieu,
        List<MembreResume> organisateurs,
        List<MembreResume> participants,
        int nbParticipants,
        boolean estInscrit,
        boolean peutModifier,
        boolean peutAnnuler,
        String motifAnnulation,
        MembreResume annulePar,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dateAnnulation,
        MembreResume creePar,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dateCreation,
        MembreResume modifiePar,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dateModification
) {
    public static EvenementResponse depuis(
            Evenement evenement,
            Membre utilisateur,
            boolean peutModifier,
            boolean peutAnnuler) {
        List<MembreResume> organisateurs = evenement.getOrganisateurs().stream()
                .map(MembreResume::depuis)
                .toList();
        List<MembreResume> participants = evenement.getParticipants().stream()
                .map(MembreResume::depuis)
                .toList();
        boolean estInscrit = utilisateur != null && evenement.getParticipants().stream()
                .anyMatch(membre -> membre.getId().equals(utilisateur.getId()));

        return new EvenementResponse(
                evenement.getId(),
                evenement.getTitre(),
                evenement.getDescription(),
                evenement.getType(),
                evenement.getStatut(),
                evenement.getEdition(),
                evenement.getDateDebut() == null ? null : evenement.getDateDebut().getYear(),
                evenement.getDateDebut(),
                evenement.getDateFin(),
                evenement.getLieu(),
                organisateurs,
                participants,
                participants.size(),
                estInscrit,
                peutModifier,
                peutAnnuler,
                evenement.getMotifAnnulation(),
                MembreResume.depuis(evenement.getAnnulePar()),
                evenement.getDateAnnulation(),
                MembreResume.depuis(evenement.getCreePar()),
                evenement.getDateCreation(),
                MembreResume.depuis(evenement.getModifiePar()),
                evenement.getDateMiseAJour()
        );
    }

    public record MembreResume(Long id, String nom, String prenom, String photo, EquipeLaboratoire equipe) {
        private static MembreResume depuis(Membre membre) {
            if (membre == null) {
                return null;
            }
            return new MembreResume(
                    membre.getId(),
                    membre.getNom(),
                    membre.getPrenom(),
                    membre.getPhoto(),
                    membre.getEquipeLaboratoire()
            );
        }
    }
}
