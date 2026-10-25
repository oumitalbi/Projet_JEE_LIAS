package com.lias.gestion_laboratoire.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lias.gestion_laboratoire.models.enums.StatutReunion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité JPA représentant une réunion organisée au sein du laboratoire (ex: Assemblée Générale,
 * Conseil de Laboratoire, réunion d'équipe).
 * 
 * <p>Une réunion peut avoir un document de type Procès-Verbal (PV) associé, et
 * rassemble plusieurs membres participants.</p>
 */
@Entity
@Table(name = "reunions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reunion {

    /** Identifiant unique auto-généré. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Titre ou objet de la réunion. */
    @Column(nullable = false)
    private String titre;

    /** Date et heure de tenue de la réunion. */
    @Column(name = "date_reunion", nullable = false)
    private LocalDateTime dateReunion;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Column(name = "lieu", length = 255)
    private String lieu;

    /** Ordre du jour détaillé de la réunion. */
    @Column(name = "ordre_du_jour", columnDefinition = "TEXT", nullable = false)
    private String ordreDuJour;

    /** Statut actuel de la réunion. */
    @Enumerated(EnumType.STRING)
    private StatutReunion statut = StatutReunion.PLANIFIEE;

    @Column(name = "archivee", nullable = false, columnDefinition = "boolean default false")
    private boolean archivee;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_avant_archivage", length = 20)
    private StatutReunion statutAvantArchivage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisateur_id")
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Membre organisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par_id")
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Membre creePar;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifie_par_id")
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Membre modifiePar;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statut_change_par_id")
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Membre statutChangePar;

    @Column(name = "date_changement_statut")
    private LocalDateTime dateChangementStatut;

    // =========================================================================
    // RELATIONS
    // =========================================================================

    /**
     * Document officiel (Procès-Verbal) associé à cette réunion une fois terminée.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_pv_id")
    @JsonIgnoreProperties("evenement") // Empêche une éventuelle boucle si le document est géré
    private Document procesVerbal;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "reunion", cascade = CascadeType.ALL)
    private List<ReunionProcesVerbal> historiqueProcesVerbaux = new ArrayList<>();

    /**
     * Ensemble des membres ayant participé ou devant participer à la réunion.
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToMany
    @JoinTable(
            name = "reunion_participants",
            joinColumns = @JoinColumn(name = "reunion_id"),
            inverseJoinColumns = @JoinColumn(name = "membre_id")
    )
    @JsonIgnoreProperties({"reunionsParticipes", "evenements", "publications", "materielsEnPossession", "mesDemandes"})
    private Set<Membre> participants = new HashSet<>();

    @PrePersist
    void initialiserCreation() {
        if (dateCreation == null) dateCreation = LocalDateTime.now();
    }

    public LocalDateTime getDateDebut() {
        return dateReunion;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateReunion = dateDebut;
    }
}
