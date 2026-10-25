package com.lias.gestion_laboratoire.models.enums;

/**
 * Enumération des types de documents gérés par l'archivage
 * documentaire du laboratoire de recherche LIAS.
 *
 * <p>Utilisée comme discriminant métier dans l'entité {@code Document}
 * pour catégoriser chaque fichier archivé et adapter les traitements
 * associés (filtrage, validation, droits d'accès…).</p>
 */
public enum TypeDocument {

    /**
     * Demande de financement liée à un événement ou à un projet
     * de recherche (ex : dossier ANR, demande de subvention).
     */
    DEMANDE_FINANCEMENT,

    /**
     * Programme officiel d'un événement scientifique
     * (planning des sessions, liste des intervenants, horaires).
     */
    PROGRAMME,

    /**
     * Attestation délivrée dans le cadre d'un événement
     * (attestation de participation, de présentation, etc.).
     */
    ATTESTATION,

    /**
     * Rapport scientifique ou administratif
     * (rapport d'activité, compte-rendu de réunion, bilan annuel).
     */
    RAPPORT,

    /** Procès-verbal officiel d'une réunion. */
    PV,

    /** Convention ou accord signé avec un partenaire. */
    CONVENTION,

    /**
     * Document administratif général non directement lié
     * à un événement spécifique (convention, courrier officiel,
     * document réglementaire).
     */
    DOCUMENT_ADMINISTRATIF
}
