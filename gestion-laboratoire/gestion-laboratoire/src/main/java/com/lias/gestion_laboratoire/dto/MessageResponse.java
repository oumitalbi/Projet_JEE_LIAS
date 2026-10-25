package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.Message;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.TypeMessage;

import java.time.LocalDateTime;

/**
 * DTO de réponse pour l'affichage d'un message dans le frontend.
 */
public record MessageResponse(
        Long id,
        TypeMessage typeMessage,
        String contenu,
        MessageMembreDTO expediteur,
        MessageMembreDTO destinataire,
        EquipeLaboratoire equipeDestinataire,
        Long evenementId,
        String evenementTitre,
        boolean lu,
        boolean archivee,
        LocalDateTime dateEnvoi
) {
    public static MessageResponse from(Message m, boolean isLuForCurrentUser) {
        if (m == null) return null;
        return new MessageResponse(
                m.getId(),
                m.getTypeMessage(),
                m.getContenu(),
                MessageMembreDTO.from(m.getExpediteur()),
                MessageMembreDTO.from(m.getDestinataire()),
                m.getEquipeDestinataire(),
                m.getEvenement() != null ? m.getEvenement().getId() : null,
                m.getEvenement() != null ? m.getEvenement().getTitre() : null,
                isLuForCurrentUser,
                m.isArchivee(),
                m.getDateEnvoi()
        );
    }
}
