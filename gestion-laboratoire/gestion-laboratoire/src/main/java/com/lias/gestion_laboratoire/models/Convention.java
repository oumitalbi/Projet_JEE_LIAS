package com.lias.gestion_laboratoire.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entité JPA représentant une convention de collaboration (contrat, accord cadre,
 * accord de confidentialité) signée entre le laboratoire LIAS et un partenaire.
 */
@Entity
@Table(name = "conventions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Convention {

    /** Identifiant unique de la convention. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Titre explicite de la convention ou de l'accord. */
    @Column(nullable = false)
    private String titre;

    /** Description détaillée de l'objet de la collaboration. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Date officielle de signature de la convention. */
    @Column(name = "date_signature")
    private LocalDate dateSignature;

    /** Date de fin ou d'expiration légale de la convention. */
    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    // =========================================================================
    // RELATIONS
    // =========================================================================

    /**
     * Le partenaire signataire de cette convention. Obligatoire.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partenaire_id", nullable = false)
    @JsonIgnoreProperties("conventions")
    private Partenaire partenaire;

    /**
     * Document numérisé représentant le contrat physique signé.
     * 
     * <p>Permet d'archiver la version PDF officielle de la convention 
     * via le module d'archivage documentaire (Document).</p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_convention_id")
    @JsonIgnoreProperties("evenement") // Sécurité de sérialisation
    private Document documentConvention;

    @ManyToMany
    @JoinTable(name = "convention_evenements",
            joinColumns = @JoinColumn(name = "convention_id"),
            inverseJoinColumns = @JoinColumn(name = "evenement_id"))
    @JsonIgnoreProperties({"participants", "organisateurs", "documents"})
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Evenement> activites = new HashSet<>();

    @Column(name = "archivee", nullable = false)
    private boolean archivee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par_id")
    @JsonIgnoreProperties({"conventions", "evenements", "publications", "motDePasse"})
    private Membre creePar;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifie_par_id")
    @JsonIgnoreProperties({"conventions", "evenements", "publications", "motDePasse"})
    private Membre modifiePar;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_par_id")
    @JsonIgnoreProperties({"conventions", "evenements", "publications", "motDePasse"})
    private Membre archivePar;

    @Column(name = "date_archivage")
    private LocalDateTime dateArchivage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaure_par_id")
    @JsonIgnoreProperties({"conventions", "evenements", "publications", "motDePasse"})
    private Membre restaurePar;

    @Column(name = "date_restauration")
    private LocalDateTime dateRestauration;

    @PrePersist
    void initialiserCreation() {
        if (dateCreation == null) dateCreation = LocalDateTime.now();
    }
}
