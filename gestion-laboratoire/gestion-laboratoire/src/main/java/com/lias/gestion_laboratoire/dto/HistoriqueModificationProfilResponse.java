package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.HistoriqueModificationProfil;

import java.time.LocalDateTime;

public record HistoriqueModificationProfilResponse(
        Long id,
        Long membreId,
        String action,
        String details,
        String auteur,
        LocalDateTime dateAction) {
    public static HistoriqueModificationProfilResponse from(HistoriqueModificationProfil h) {
        String auteur = h.getEffectuePar() == null ? null
                : h.getEffectuePar().getPrenom() + " " + h.getEffectuePar().getNom();
        return new HistoriqueModificationProfilResponse(h.getId(), h.getMembre().getId(),
                h.getAction(), h.getDetails(), auteur, h.getDateAction());
    }
}
