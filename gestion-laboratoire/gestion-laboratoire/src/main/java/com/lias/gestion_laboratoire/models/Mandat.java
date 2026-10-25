package com.lias.gestion_laboratoire.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entité JPA représentant un mandat électif ou nominatif (directeur, responsable d'équipe...)
 * exercé par un membre du laboratoire au cours du temps.
 *
 * <p>Permet de conserver l'historique complet des responsabilités assumées
 * par les chercheurs du laboratoire LIAS.</p>
 */
@Entity
@Table(name = "mandats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mandat {

    /** Identifiant unique du mandat. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Date de prise de fonction officielle. */
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    /** 
     * Date de fin de fonction. 
     * <p>Si ce champ est {@code null}, le mandat est considéré comme toujours en cours.</p>
     */
    @Column(name = "date_fin")
    private LocalDate dateFin;

    /** Le rôle ou la responsabilité assumée durant ce mandat. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role responsabilite;

    /** Précisions éventuelles sur les missions spécifiques liées à ce mandat. */
    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipe", length = 20)
    private EquipeLaboratoire equipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribue_par_id")
    private Membre attribuePar;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloture_par_id")
    private Membre cloturePar;

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @Column(name = "motif_cloture", length = 1000)
    private String motifCloture;

    // =========================================================================
    // RELATIONS
    // =========================================================================

    /**
     * Le membre du laboratoire exerçant ou ayant exercé ce mandat.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membre_id", nullable = false)
    @JsonIgnoreProperties({"historiqueMandats", "reunionsParticipes", "evenements", "publications", "materielsEnPossession", "mesDemandes"})
    private Membre titulaire;

    @PrePersist
    void initialiserCreation() {
        if (dateCreation == null) dateCreation = LocalDateTime.now();
    }
}
