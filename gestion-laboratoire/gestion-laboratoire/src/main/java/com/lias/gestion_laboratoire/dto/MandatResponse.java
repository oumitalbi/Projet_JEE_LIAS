package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.Mandat;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MandatResponse(
        Long id,
        MembreSummary membre,
        Role responsabilite,
        EquipeLaboratoire equipe,
        LocalDate dateDebut,
        LocalDate dateFin,
        boolean actif,
        String description,
        MembreSummary attribuePar,
        LocalDateTime dateCreation,
        MembreSummary cloturePar,
        LocalDateTime dateCloture,
        String motifCloture) {

    public static MandatResponse from(Mandat mandat, LocalDate reference) {
        return new MandatResponse(
                mandat.getId(),
                MembreSummary.from(mandat.getTitulaire()),
                mandat.getResponsabilite(),
                mandat.getEquipe(),
                mandat.getDateDebut(),
                mandat.getDateFin(),
                !mandat.getDateDebut().isAfter(reference)
                        && (mandat.getDateFin() == null || !mandat.getDateFin().isBefore(reference)),
                mandat.getDescription(),
                MembreSummary.from(mandat.getAttribuePar()),
                mandat.getDateCreation(),
                MembreSummary.from(mandat.getCloturePar()),
                mandat.getDateCloture(),
                mandat.getMotifCloture());
    }

    public record MembreSummary(Long id, String nom, String prenom, String photo,
                                EquipeLaboratoire equipeLaboratoire) {
        static MembreSummary from(com.lias.gestion_laboratoire.models.Membre membre) {
            return membre == null ? null : new MembreSummary(
                    membre.getId(), membre.getNom(), membre.getPrenom(), membre.getPhoto(),
                    membre.getEquipeLaboratoire());
        }
    }
}
