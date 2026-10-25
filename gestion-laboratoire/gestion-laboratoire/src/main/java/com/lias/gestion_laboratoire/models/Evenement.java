package com.lias.gestion_laboratoire.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lias.gestion_laboratoire.models.enums.StatutEvenement;
import com.lias.gestion_laboratoire.models.enums.TypeEvenement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entité JPA représentant un événement scientifique organisé ou suivi
 * par le laboratoire de recherche LIAS.
 *
 * <p>Un événement possède un type ({@link TypeEvenement}), une période définie
 * par {@code dateDebut} et {@code dateFin}, un lieu, et un ensemble de membres
 * participants ainsi qu'un ensemble d'organisateurs, chacun lié via une
 * relation {@code @ManyToMany} distincte.</p>
 *
 * <p><strong>Règle de Soft Delete :</strong> Les événements ne sont jamais
 * physiquement supprimés de la base de données. Le champ {@code estAnnule}
 * permet de les annuler logiquement tout en conservant l'historique.</p>
 *
 * <p>La table de jointure {@code evenement_participants} matérialise la
 * relation Many-to-Many entre les événements et les membres.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "evenements")
public class Evenement {

    // =========================================================================
    // IDENTIFIANT
    // =========================================================================

    /** Identifiant unique auto-généré (clé primaire). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // INFORMATIONS GÉNÉRALES
    // =========================================================================

    /**
     * Titre de l'événement scientifique.
     * Limité à 200 caractères, obligatoire.
     */
    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    /**
     * Description détaillée de l'événement : objectifs, programme, intervenants.
     * Utilise {@code TEXT} en base pour autoriser des contenus longs.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Lieu de tenue de l'événement (ville, salle, établissement, lien visio…).
     */
    @Column(name = "lieu")
    private String lieu;

    @Column(name = "edition", length = 100)
    private String edition;

    // =========================================================================
    // CHAMPS TEMPORELS
    // =========================================================================

    /**
     * Date et heure de début de l'événement.
     * Obligatoire. Sérialisée au format ISO-8601 sans décalage horaire.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    /**
     * Date et heure de fin de l'événement.
     * Obligatoire. Doit être postérieure à {@code dateDebut}.
     * Sérialisée au format ISO-8601 sans décalage horaire.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "date_fin", nullable = false)
    private LocalDateTime dateFin;

    // =========================================================================
    // ENUM MÉTIER
    // =========================================================================

    /**
     * Catégorie de l'événement scientifique.
     * Stockée sous forme de chaîne de caractères en base ({@code EnumType.STRING})
     * pour la lisibilité des données et la robustesse face aux refactorisations.
     *
     * @see TypeEvenement
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 30)
    private TypeEvenement type;

    // =========================================================================
    // SOFT DELETE
    // =========================================================================

    /**
     * Indicateur d'annulation de l'événement.
     *
     * <p><strong>Règle métier :</strong> Les événements ne sont jamais supprimés
     * physiquement. Passer ce champ à {@code true} annule l'événement logiquement
     * tout en conservant l'intégralité des données historiques en base.</p>
     *
     * <p>Valeur par défaut : {@code false} (événement actif à la création).</p>
     */
    @Column(name = "est_annule", nullable = false)
    private boolean estAnnule = false;

    @Column(name = "motif_annulation", length = 2000)
    private String motifAnnulation;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annule_par_id")
    private Membre annulePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par_id")
    private Membre creePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifie_par_id")
    private Membre modifiePar;

    // =========================================================================
    // RELATION MANY-TO-MANY — PARTICIPANTS
    // =========================================================================

    /**
     * Ensemble des membres du laboratoire participant à cet événement.
     *
     * <p>Cette relation est la <strong>côté propriétaire</strong> (owner side)
     * de l'association avec {@link Membre}. Elle est matérialisée par la table
     * de jointure {@code evenement_participants}.</p>
     *
     * <p>{@code @JsonIgnoreProperties("evenements")} empêche la sérialisation
     * récursive du champ {@code evenements} présent dans chaque {@link Membre},
     * évitant ainsi toute boucle infinie Jackson lors de la sérialisation JSON.</p>
     *
     * @see Membre#evenements
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToMany
    @JoinTable(
            name = "evenement_participants",
            joinColumns = @JoinColumn(name = "evenement_id"),
            inverseJoinColumns = @JoinColumn(name = "membre_id")
    )
    @JsonIgnoreProperties("evenements")
    private Set<Membre> participants = new HashSet<>();

    // =========================================================================
    // RELATION MANY-TO-MANY — ORGANISATEURS
    // =========================================================================

    /**
     * Membres responsables de l'organisation et de la gestion de cet événement.
     * Cette relation est distincte des participants et matérialisée par la table
     * de jointure {@code evenement_organisateurs}.
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToMany
    @JoinTable(
            name = "evenement_organisateurs",
            joinColumns = @JoinColumn(name = "evenement_id"),
            inverseJoinColumns = @JoinColumn(name = "membre_id")
    )
    @JsonIgnoreProperties("evenementsOrganises")
    private Set<Membre> organisateurs = new HashSet<>();

    // =========================================================================
    // RELATION ONE-TO-MANY — DOCUMENTS
    // =========================================================================

    /**
     * Liste des documents archivés rattachés à cet événement.
     *
     * <p>Côté inverse (mappedBy) de la relation bidirectionnelle
     * {@link ManyToOne} gérée depuis l'entité {@link Document}. Les documents
     * restent conservés indépendamment du cycle de vie de l'événement.</p>
     *
     * <p>{@code @EqualsAndHashCode.Exclude} et {@code @ToString.Exclude}
     * empêchent Lombok de parcourir cette collection lors du calcul de
     * {@code hashCode()}, {@code equals()} ou {@code toString()}, évitant
     * toute {@link java.util.ConcurrentModificationException} avec Hibernate.</p>
     *
     * @see Document#evenement
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    @OneToMany(mappedBy = "evenement")
    private List<Document> documents = new ArrayList<>();

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

    @Transient
    public StatutEvenement getStatut() {
        if (estAnnule) {
            return StatutEvenement.ANNULE;
        }
        if (dateDebut == null || dateFin == null) {
            return null;
        }
        LocalDateTime maintenant = LocalDateTime.now();
        if (maintenant.isBefore(dateDebut)) {
            return StatutEvenement.A_VENIR;
        }
        if (!maintenant.isAfter(dateFin)) {
            return StatutEvenement.EN_COURS;
        }
        return StatutEvenement.PASSE;
    }
}
