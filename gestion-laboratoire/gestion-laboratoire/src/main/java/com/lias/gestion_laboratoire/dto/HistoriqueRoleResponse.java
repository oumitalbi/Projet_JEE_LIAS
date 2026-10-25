package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.HistoriqueRole;

import java.time.LocalDateTime;

public record HistoriqueRoleResponse(
        Long id,
        Long membreId,
        String ancienRole,
        String nouveauRole,
        String ancienneEquipe,
        String nouvelleEquipe,
        String motif,
        String auteur,
        LocalDateTime dateChangement) {
    public static HistoriqueRoleResponse from(HistoriqueRole h) {
        String auteur = h.getModifiePar() == null ? null
                : h.getModifiePar().getPrenom() + " " + h.getModifiePar().getNom();
        return new HistoriqueRoleResponse(h.getId(), h.getMembre().getId(),
                h.getAncienRole() == null ? null : h.getAncienRole().name(),
                h.getNouveauRole().name(),
                h.getAncienneEquipe() == null ? null : h.getAncienneEquipe().name(),
                h.getNouvelleEquipe() == null ? null : h.getNouvelleEquipe().name(),
                h.getMotif(), auteur, h.getDateChangement());
    }
}
