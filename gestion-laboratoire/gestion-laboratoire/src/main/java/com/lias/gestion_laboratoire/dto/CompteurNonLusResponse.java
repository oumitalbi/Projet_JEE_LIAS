package com.lias.gestion_laboratoire.dto;

/**
 * DTO renvoyant les totaux de messages et notifications non lus pour les badges d'en-tête et de menu.
 */
public record CompteurNonLusResponse(
        long notifications,
        long messagesTotal,
        long messagesDirect,
        long messagesEquipe,
        long messagesEvenement
) {}
