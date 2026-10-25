package com.lias.gestion_laboratoire.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lias.gestion_laboratoire.models.enums.EtatMateriel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entité JPA représentant un équipement matériel appartenant au laboratoire LIAS.
 * 
 * <p>Un matériel peut être assigné temporairement ou de façon permanente
 * à un membre spécifique du laboratoire pour ses recherches.</p>
 */
@Entity
@Table(name = "materiels")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Materiel {

    /** Identifiant unique auto-généré. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom ou libellé du matériel (ex: Ordinateur Dell XPS, Microscope). */
    @Column(nullable = false)
    private String nom;

    /** Description détaillée des caractéristiques du matériel. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Numéro d'inventaire unique attribué par le laboratoire ou l'université. */
    @Column(name = "numero_inventaire", unique = true, nullable = false)
    private String numeroInventaire;

    @Column(name = "categorie", length = 80)
    private String categorie = "Autre";

    @Column(name = "quantite_totale")
    private Integer quantiteTotale = 1;

    @Column(name = "quantite_disponible")
    private Integer quantiteDisponible = 1;

    @Column(name = "consommable")
    private Boolean consommable = false;

    @Column(name = "archivee")
    private Boolean archivee = false;

    /** L'état physique actuel du matériel. */
    @Enumerated(EnumType.STRING)
    private EtatMateriel etat;

    /** Date à laquelle le laboratoire a acquis le matériel. */
    @Column(name = "date_acquisition")
    private LocalDate dateAcquisition;

    /**
     * Membre du laboratoire auquel le matériel est actuellement affecté.
     * <p>Peut être null si le matériel est stocké en réserve.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membre_id")
    @JsonIgnoreProperties({"materielsEnPossession", "mesDemandes", "publications", "evenements"})
    private Membre membreAffecte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par_id")
    private Membre creePar;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifie_par_id")
    private Membre modifiePar;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_par_id")
    private Membre archivePar;

    @Column(name = "date_archivage")
    private LocalDateTime dateArchivage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaure_par_id")
    private Membre restaurePar;

    @Column(name = "date_restauration")
    private LocalDateTime dateRestauration;

    @PrePersist
    void initialiserDateCreation() {
        if (dateCreation == null) dateCreation = LocalDateTime.now();
        if (categorie == null || categorie.isBlank()) categorie = "Autre";
        if (quantiteTotale == null) quantiteTotale = 1;
        if (quantiteDisponible == null) quantiteDisponible = quantiteTotale;
        if (consommable == null) consommable = false;
        if (archivee == null) archivee = false;
    }

    public boolean isConsommable() {
        return Boolean.TRUE.equals(consommable);
    }

    public boolean isArchivee() {
        return Boolean.TRUE.equals(archivee);
    }

    public String getCategorie() {
        return categorie == null || categorie.isBlank() ? "Autre" : categorie;
    }

    public Integer getQuantiteTotale() {
        return quantiteTotale == null ? 1 : quantiteTotale;
    }

    public Integer getQuantiteDisponible() {
        return quantiteDisponible == null
                ? (membreAffecte == null ? getQuantiteTotale() : 0)
                : quantiteDisponible;
    }
}
