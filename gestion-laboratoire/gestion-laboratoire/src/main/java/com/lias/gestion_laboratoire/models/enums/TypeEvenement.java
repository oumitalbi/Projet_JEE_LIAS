package com.lias.gestion_laboratoire.models.enums;

/**
 * Enumération des types d'événements scientifiques organisés
 * ou auxquels participent les membres du laboratoire de recherche LIAS.
 *
 * <p>Utilisée comme discriminant métier dans l'entité {@code Evenement}
 * pour catégoriser chaque événement et adapter les traitements associés.</p>
 */
public enum TypeEvenement {

    /**
     * Conférence scientifique internationale ou nationale.
     * Rassemble des chercheurs autour d'un thème commun
     * avec présentation d'articles peer-reviewed.
     */
    CONFERENCE,

    /**
     * Séminaire de recherche, généralement interne au laboratoire
     * ou inter-laboratoires, portant sur des travaux en cours.
     */
    SEMINAIRE,

    /**
     * Soutenance de thèse de doctorat.
     * Marque la validation officielle des travaux d'un doctorant
     * devant un jury constitué.
     */
    SOUTENANCE_THESE,

    /**
     * Atelier de travail (workshop) pratique ou thématique.
     * Format interactif favorisant les échanges techniques
     * et la collaboration entre participants.
     */
    WORKSHOP
}
