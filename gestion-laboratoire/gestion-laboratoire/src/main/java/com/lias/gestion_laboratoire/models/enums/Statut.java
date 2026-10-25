package com.lias.gestion_laboratoire.models.enums;

/**
 * Statut professionnel d'un membre du laboratoire de recherche.
 */
public enum Statut {

    /** Membre permanent du laboratoire (CDI ou titulaire). */
    PERMANENT,

    /** Membre associé, en collaboration partielle. */
    ASSOCIE,

    /** Doctorant inscrit en thèse au sein du laboratoire. */
    DOCTORANT,

    /** Membre ayant pris sa retraite mais toujours référencé. */
    RETRAITE,

    /** Ancien membre, qui a quitté le laboratoire. */
    ANCIEN
}
