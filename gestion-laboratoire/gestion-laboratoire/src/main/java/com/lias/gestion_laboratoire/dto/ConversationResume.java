package com.lias.gestion_laboratoire.dto;

import java.time.LocalDateTime;

/**
 * DTO résumant une conversation directe pour la liste latérale de la messagerie.
 */
public record ConversationResume(
        Long autreMembreId,
        String autreMembreNom,
        String autreMembrePrenom,
        String autreMembreEmail,
        String autreMembrePhoto,
        String autreMembreRole,
        String dernierMessage,
        LocalDateTime dateDernierMessage,
        long nonLus
) {}
