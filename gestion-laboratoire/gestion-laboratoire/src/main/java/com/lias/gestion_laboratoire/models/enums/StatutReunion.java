package com.lias.gestion_laboratoire.models.enums;

/**
 * Enumération représentant les différents statuts d'une réunion au sein du laboratoire.
 */
public enum StatutReunion {
    /** La réunion est programmée pour une date future. */
    PLANIFIEE,
    
    /** La réunion est actuellement en cours de déroulement. */
    EN_COURS,
    
    /** La réunion est terminée et le PV peut être rédigé/associé. */
    TERMINEE,
    
    /** La réunion a été annulée. */
    ANNULEE,

    /** Réunion conservée dans les archives et masquée de l'agenda actif. */
    ARCHIVEE
}
