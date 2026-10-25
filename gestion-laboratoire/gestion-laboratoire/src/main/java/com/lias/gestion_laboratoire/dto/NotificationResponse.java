package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.Notification;
import com.lias.gestion_laboratoire.models.enums.TypeNotification;

import java.time.LocalDateTime;

/**
 * DTO de réponse pour l'affichage d'une notification dans le frontend.
 */
public record NotificationResponse(
        Long id,
        String titre,
        String contenu,
        TypeNotification typeNotification,
        String cibleType,
        Long cibleId,
        boolean lue,
        boolean archivee,
        LocalDateTime dateCreation
) {
    public static NotificationResponse from(Notification n) {
        if (n == null) return null;
        return new NotificationResponse(
                n.getId(),
                n.getTitre(),
                n.getContenu(),
                n.getTypeNotification(),
                n.getCibleType(),
                n.getCibleId(),
                n.isLue(),
                n.isArchivee(),
                n.getDateCreation()
        );
    }
}
