package com.lias.gestion_laboratoire.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lias.gestion_laboratoire.models.enums.StatutDemande;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entité JPA représentant une demande de matériel effectuée par un membre
 * du laboratoire LIAS.
 */
@Entity
@Table(name = "demandes_materiel")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandeMateriel {

    /** Identifiant unique de la demande. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Description détaillée du besoin (ce que le chercheur demande et
     * pour quel projet).
     */
    @Column(name = "description_besoin", nullable = false, columnDefinition = "TEXT")
    private String descriptionBesoin;

    @Column(name = "nom_besoin", length = 200)
    private String nomBesoin;

    @Column(name = "quantite_demandee")
    private Integer quantiteDemandee = 1;

    @Column(name = "motif_demande", columnDefinition = "TEXT")
    private String motifDemande;

    @Column(name = "motif_refus", columnDefinition = "TEXT")
    private String motifRefus;

    @Column(name = "quantite_retournee")
    private Integer quantiteRetournee = 0;

    @Column(name = "date_retour")
    private LocalDateTime dateRetour;

    /** Date à laquelle la demande a été soumise. */
    @CreationTimestamp
    @Column(name = "date_demande", updatable = false)
    private LocalDateTime dateDemande;

    /** Date à laquelle la direction a répondu à la demande. */
    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;

    /** Statut actuel du traitement de la demande. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDemande statut = StatutDemande.EN_ATTENTE;

    /** Membre ayant formulé la demande. Obligatoire. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandeur_id", nullable = false)
    @JsonIgnoreProperties({"mesDemandes", "materielsEnPossession", "publications", "evenements"})
    private Membre demandeur;

    /**
     * Matériel physique spécifiquement assigné à cette demande une fois
     * qu'elle a été validée par la direction.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materiel_id")
    private Materiel materielAssigne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materiel_demande_id")
    private Materiel materielDemande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traite_par_id")
    private Membre traitePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retourne_par_id")
    private Membre retournePar;

    @PrePersist
    @PostLoad
    void normaliserQuantites() {
        if (quantiteDemandee == null) quantiteDemandee = 1;
        if (quantiteRetournee == null) quantiteRetournee = 0;
    }
}
