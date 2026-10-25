package com.lias.gestion_laboratoire.models.enums;

/**
 * Type de publication scientifique produite par les membres
 * du laboratoire de recherche LIAS.
 */
public enum TypePublication {

    /** Article publié dans un journal ou une revue scientifique. */
    ARTICLE_JOURNAL,

    /** Article présenté lors d'une conférence scientifique. */
    CONFERENCE,

    /** Chapitre contribué dans un ouvrage collectif. */
    CHAPITRE_LIVRE,

    /** Thèse de doctorat soutenue au sein du laboratoire. */
    THESE,

    /** Brevet d'invention déposé. */
    BREVET
}
