package com.lias.gestion_laboratoire.models.enums;

/**
 * Enumération définissant la nature juridique ou le secteur d'activité
 * d'un partenaire avec lequel le laboratoire LIAS collabore.
 */
public enum TypePartenaire {
    /** Partenaire issu du monde universitaire ou de la recherche académique. */
    ACADEMIQUE,
    
    /** Partenaire issu du secteur privé (entreprise, startup, industrie). */
    INDUSTRIEL,
    
    /** Partenaire de type organisme public, ministère, région ou agence d'État. */
    INSTITUTIONNEL
}
