package com.lias.gestion_laboratoire.models.enums;

/**
 * Rôle fonctionnel d'un membre au sein du laboratoire de recherche.
 */
public enum Role {

    /** Directeur général du laboratoire. */
    DIRECTEUR,

    /** Vice-directeur du laboratoire. */
    VICE_DIRECTEUR,

    /** Chef d'une équipe de recherche. */
    CHEF_EQUIPE,

    /** Membre actif d'une équipe de recherche. */
    MEMBRE_EQUIPE,

    /** Administrateur (gestion et support). */
    ADMIN
}
