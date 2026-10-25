package com.lias.gestion_laboratoire.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.Statut;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entité JPA représentant un membre du laboratoire de recherche LIAS.
 *
 * <p>Règle de non-suppression : les membres ne sont jamais physiquement supprimés
 * de la base de données. Le champ {@code compteActif} permet de les désactiver
 * logiquement (soft delete).</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "membres")
public class Membre {

    // =========================================================================
    // IDENTIFIANT
    // =========================================================================

    /** Identifiant unique auto-généré (clé primaire). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // INFORMATIONS PERSONNELLES
    // =========================================================================

    /**
     * Titre académique du membre (ex : Pr., Dr., M., Mme.).
     * Stocké sous forme de chaîne pour une saisie libre.
     */
    @Column(name = "titre_academique", length = 20)
    private String titreAcademique;

    /** Nom de famille du membre. */
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    /** Prénom du membre. */
    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    /** Adresse e-mail professionnelle — doit être unique dans le système. */
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * URL ou chemin vers la photo de profil du membre.
     * Peut pointer vers un stockage distant (S3, CDN) ou local.
     */
    @Column(name = "photo_url", length = 500)
    private String photo;

    /**
     * Biographie ou présentation du membre.
     * Utilise {@code TEXT} en base pour autoriser de longs contenus.
     */
    @Column(name = "biographie", columnDefinition = "TEXT")
    private String biographie;

    /** Établissement d'origine ou d'affiliation principale du membre. */
    @Column(name = "etablissement_origine", length = 200)
    private String etablissementOrigine;

    @Column(name = "laboratoire_origine", length = 200)
    private String laboratoireOrigine;

    @Column(name = "centres_interet", columnDefinition = "TEXT")
    private String centresInteret;

    // =========================================================================
    // DATES
    // =========================================================================

    /** Date de naissance du membre. */
    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    /** Date d'embauche ou de recrutement au sein du laboratoire. */
    @Column(name = "date_embauche")
    private LocalDate dateEmbauche;

    /** Date d'affiliation officielle au laboratoire. */
    @Column(name = "date_affiliation")
    private LocalDate dateAffiliation;

    // =========================================================================
    // ENUMS MÉTIER
    // =========================================================================

    /**
     * Statut professionnel du membre au sein du laboratoire.
     *
     * @see Statut
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private Statut statut;

    /**
     * Rôle fonctionnel du membre dans l'organisation du laboratoire.
     *
     * @see Role
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    /**
     * Équipe de recherche à laquelle le membre est rattaché.
     * Par défaut {@link EquipeLaboratoire#AUCUNE} si non assigné.
     *
     * @see EquipeLaboratoire
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "equipe_laboratoire", nullable = false, length = 10)
    private EquipeLaboratoire equipeLaboratoire = EquipeLaboratoire.AUCUNE;

    // =========================================================================
    // RELATION MANY-TO-MANY — PUBLICATIONS
    // =========================================================================

    /**
     * Ensemble des publications scientifiques co-signées par ce membre.
     *
     * <p>Côté inverse (mappedBy) de la relation bidirectionnelle
     * {@link ManyToMany} gérée depuis l'entité {@link Publication}.</p>
     *
     * <p>{@code @JsonIgnore} empêche la sérialisation de ce champ dans les
     * réponses API du endpoint {@code /api/membres}, évitant ainsi toute
     * boucle infinie de sérialisation Jackson.</p>
     *
     * @see Publication#auteurs
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToMany(mappedBy = "auteurs")
    @JsonIgnore
    private Set<Publication> publications = new HashSet<>();

    // =========================================================================
    // RELATION MANY-TO-MANY — ÉVÉNEMENTS SCIENTIFIQUES
    // =========================================================================

    /**
     * Ensemble des événements scientifiques auxquels ce membre participe.
     *
     * <p>Côté inverse (mappedBy) de la relation bidirectionnelle
     * {@link ManyToMany} gérée depuis l'entité {@link Evenement}.
     * La table de jointure {@code evenement_participants} est définie
     * et gérée exclusivement côté {@code Evenement}.</p>
     *
     * <p>{@code @JsonIgnore} empêche la sérialisation de ce champ dans les
     * réponses API du endpoint {@code /api/membres}, évitant ainsi toute
     * boucle infinie de sérialisation Jackson.</p>
     *
     * @see Evenement#participants
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToMany(mappedBy = "participants")
    @JsonIgnore
    private Set<Evenement> evenements = new HashSet<>();

    /**
     * Événements organisés par ce membre, côté inverse de
     * {@link Evenement#organisateurs}.
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToMany(mappedBy = "organisateurs")
    @JsonIgnore
    private Set<Evenement> evenementsOrganises = new HashSet<>();

    // =========================================================================
    // MODULE LOGISTIQUE — MATÉRIEL & DEMANDES
    // =========================================================================

    /**
     * Liste des équipements matériels actuellement en possession du membre.
     * Côté inverse de la relation @ManyToOne définie dans Materiel.
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "membreAffecte")
    @JsonIgnore
    private List<Materiel> materielsEnPossession = new ArrayList<>();

    /**
     * Historique de toutes les demandes de matériel formulées par ce membre.
     * La suppression d'un membre entraîne la suppression en cascade de ses demandes.
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "demandeur", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<DemandeMateriel> mesDemandes = new ArrayList<>();

    // =========================================================================
    // MODULE RÉUNIONS
    // =========================================================================

    /**
     * Ensemble des réunions auxquelles le membre est convié ou a participé.
     * Côté inverse de la relation @ManyToMany définie dans Reunion.
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToMany(mappedBy = "participants")
    @JsonIgnore
    private Set<Reunion> reunionsParticipes = new HashSet<>();

    // =========================================================================
    // MODULE HISTORIQUE & MANDATS
    // =========================================================================

    /**
     * Historique des responsabilités (directeur, responsable d'équipe...) assumées
     * par le membre au sein du laboratoire au fil du temps.
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "titulaire", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Mandat> historiqueMandats = new ArrayList<>();

    // =========================================================================
    // SÉCURITÉ ET GESTION DU COMPTE
    // =========================================================================

    /**
     * Mot de passe du membre pour l'authentification.
     * Exclu de la sérialisation JSON pour des raisons de sécurité.
     */
    @JsonIgnore
    @Column(name = "mot_de_passe")
    private String motDePasse;

    /**
     * Rôle système pour la gestion des accès (Spring Security).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role_systeme", length = 30)
    private com.lias.gestion_laboratoire.models.enums.RoleUtilisateur roleSysteme;

    /**
     * Indicateur d'activité du compte membre.
     *
     * <p><strong>Règle métier :</strong> Les membres ne sont jamais supprimés
     * physiquement. Mettre ce champ à {@code false} désactive le compte tout
     * en conservant l'historique complet en base de données.</p>
     *
     * <p>Valeur par défaut : {@code true} (compte actif à la création).</p>
     */
    @Column(name = "compte_actif", nullable = false)
    private boolean compteActif = true;

    /**
     * Indique si le compte est gelé.
     * Utilisé pour les retraités dont l'historique est conservé mais l'accès bloqué.
     */
    @Column(name = "compte_gele", nullable = false)
    private boolean compteGele = false;

    @JsonIgnore
    @Column(name = "session_version", nullable = false, columnDefinition = "integer default 0")
    private int sessionVersion = 0;

    // =========================================================================
    // AUDIT — TIMESTAMPS AUTOMATIQUES
    // =========================================================================

    /**
     * Date et heure de création de l'enregistrement.
     * Alimentée automatiquement par Hibernate lors du premier {@code persist}.
     * Non modifiable après création ({@code updatable = false}).
     */
    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    /**
     * Date et heure de la dernière mise à jour de l'enregistrement.
     * Mise à jour automatiquement par Hibernate à chaque {@code merge}.
     */
    @UpdateTimestamp
    @Column(name = "date_mise_a_jour", nullable = false)
    private LocalDateTime dateMiseAJour;
}
