package com.lias.gestion_laboratoire.models.enums;

/**
 * Types de notifications générées par le système LIAS.
 * Utilisé pour le filtrage et l'affichage d'icône dans l'interface.
 */
public enum TypeNotification {

    /** Notifications liées aux événements scientifiques. */
    EVENEMENT,

    /** Notifications liées aux documents internes. */
    DOCUMENT,

    /** Notifications liées aux réunions. */
    REUNION,

    /** Notifications liées aux conventions. */
    CONVENTION,

    /** Notifications liées à la logistique et au matériel. */
    LOGISTIQUE,

    /** Notifications liées aux mandats de responsabilité. */
    MANDAT,

    /** Notifications liées aux demandes d'adhésion. */
    ADHESION,

    /** Notifications liées aux publications scientifiques. */
    PUBLICATION,

    /** Notifications liées aux messages internes. */
    MESSAGE,

    /** Notifications système générales. */
    SYSTEME
}
