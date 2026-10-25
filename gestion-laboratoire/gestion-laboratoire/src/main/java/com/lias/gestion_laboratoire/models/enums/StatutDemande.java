package com.lias.gestion_laboratoire.models.enums;

/**
 * Enumération des différents statuts d'une demande de matériel.
 */
public enum StatutDemande {
    /** La demande vient d'être créée et est en attente de validation. */
    EN_ATTENTE,
    
    /** La demande a été validée par la direction. */
    VALIDEE,
    
    /** La demande a été refusée. */
    REFUSEE,
    
    /** Le matériel demandé a été livré au membre. */
    LIVREE,
    
    /** Le matériel a été retourné au laboratoire après utilisation. */
    RETOURNEE
}
