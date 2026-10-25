package com.lias.gestion_laboratoire.models.enums;

/**
 * Types de messages échangés au sein du laboratoire LIAS.
 */
public enum TypeMessage {

    /** Message direct entre deux membres. */
    DIRECT,

    /** Message envoyé à une équipe de recherche. */
    EQUIPE,

    /** Message lié à un événement scientifique (fil de discussion). */
    EVENEMENT
}
