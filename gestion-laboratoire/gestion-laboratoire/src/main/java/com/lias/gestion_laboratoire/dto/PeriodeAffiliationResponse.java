package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.HistoriqueAffiliation;

import java.time.LocalDate;

public record PeriodeAffiliationResponse(Long id, LocalDate dateDebut, LocalDate dateFin,
                                         String statut, String motif) {
    public static PeriodeAffiliationResponse from(HistoriqueAffiliation h) {
        return new PeriodeAffiliationResponse(h.getId(), h.getDateDebut(), h.getDateFin(),
                h.getStatut().name(), h.getMotif());
    }
}
