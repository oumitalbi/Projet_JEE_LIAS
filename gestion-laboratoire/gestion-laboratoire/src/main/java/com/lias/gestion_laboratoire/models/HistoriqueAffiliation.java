package com.lias.gestion_laboratoire.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lias.gestion_laboratoire.models.enums.Statut;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Entité JPA représentant une période d'affiliation d'un membre
 * au laboratoire de recherche LIAS.
 *
 * <p>Chaque changement de statut d'un membre (départ, retour, retraite)
 * génère une nouvelle ligne dans l'historique. La période en cours a
 * {@code dateFin = null}.</p>
 *
 * <p><strong>Traçabilité :</strong> Conformément au cahier des charges,
 * chaque période d'affiliation est enregistrée avec ses dates de début
 * et de fin, permettant de retracer l'intégralité du parcours d'un
 * membre au sein du laboratoire.</p>
 *
 * @author LIAS Laboratory
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "historique_affiliations")
public class HistoriqueAffiliation {

    // =========================================================================
    // IDENTIFIANT
    // =========================================================================

    /** Identifiant unique auto-généré (clé primaire). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // RELATION — MEMBRE
    // =========================================================================

    /**
     * Membre concerné par cette période d'affiliation.
     * Obligatoire : chaque historique est rattaché à un membre précis.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "membre_id", nullable = false)
    @JsonIgnoreProperties({"publications", "evenements", "materielsEnPossession",
            "mesDemandes", "reunionsParticipes", "historiqueMandats", "motDePasse"})
    private Membre membre;

    // =========================================================================
    // PÉRIODE D'AFFILIATION
    // =========================================================================

    /**
     * Date de début de cette période d'affiliation.
     * Correspond à la date d'entrée ou de retour du membre dans le laboratoire.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    /**
     * Date de fin de cette période d'affiliation.
     * {@code null} si la période est toujours en cours (membre actuellement affilié).
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "date_fin")
    private LocalDate dateFin;

    // =========================================================================
    // STATUT DURANT CETTE PÉRIODE
    // =========================================================================

    /**
     * Statut professionnel du membre durant cette période d'affiliation.
     *
     * @see Statut
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private Statut statut;

    // =========================================================================
    // MOTIF DU CHANGEMENT
    // =========================================================================

    /**
     * Description du motif de ce changement de statut ou d'affiliation.
     * Exemples : « Départ en retraite », « Fin de contrat doctoral »,
     * « Retour après détachement ».
     */
    @Column(name = "motif", length = 500)
    private String motif;
}
