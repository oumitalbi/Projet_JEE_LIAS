package com.lias.gestion_laboratoire.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object) utilisé pour la mise à jour du profil personnel d'un membre.
 *
 * <p>Conformément aux règles métier (cahier des charges, sections 3 et 21), ce DTO
 * ne contient <strong>que</strong> les champs que le membre est autorisé à modifier
 * lui-même de manière autonome.</p>
 *
 * <p>Les champs relevant de la gouvernance (rôle, statut, équipe, état du compte, email)
 * sont volontairement omis de cet objet de transfert pour des raisons de sécurité.</p>
 *
 * @author LIAS Laboratory
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembreUpdateProfilRequest {

    /** Titre académique du membre (ex : Pr., Dr., M., Mme.). */
    private String titreAcademique;

    /** Nom de famille du membre. */
    private String nom;

    /** Prénom du membre. */
    private String prenom;

    /** URL ou chemin vers la photo de profil du membre. */
    private String photo;

    /** Biographie ou présentation personnelle. */
    private String biographie;

    /** Établissement d'origine ou d'affiliation principale. */
    private String etablissementOrigine;
}
