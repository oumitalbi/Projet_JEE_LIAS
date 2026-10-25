package com.lias.gestion_laboratoire.models;

import com.lias.gestion_laboratoire.models.enums.StatutAdhesion;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.Statut;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "demandes_adhesion", indexes = {
        @Index(name = "idx_adhesion_statut_date", columnList = "statut,date_demande"),
        @Index(name = "idx_adhesion_email_statut", columnList = "email,statut")
})
public class DemandeAdhesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;
    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "lettre_motivation", nullable = false, columnDefinition = "TEXT")
    private String lettreMotivation;

    @Column(name = "cv_chemin", length = 500)
    private String cvChemin;

    @Column(name = "cv_url", length = 500)
    private String cvUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_souhaite", nullable = false, length = 20)
    private Statut statutSouhaite;

    @Column(name = "etablissement_origine", nullable = false, length = 200)
    private String etablissementOrigine;

    @Column(name = "laboratoire_origine", length = 200)
    private String laboratoireOrigine;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipe_souhaitee", nullable = false, length = 20)
    private EquipeLaboratoire equipeSouhaitee;

    @Column(name = "centres_interet", columnDefinition = "TEXT")
    private String centresInteret;

    @Column(name = "biographie", columnDefinition = "TEXT")
    private String biographie;

    @Column(name = "consentement_donnees", nullable = false)
    private boolean consentementDonnees;

    @Column(name = "consentement_date")
    private LocalDateTime consentementDate;

    @CreationTimestamp
    @Column(name = "date_demande", nullable = false, updatable = false)
    private LocalDateTime dateDemande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutAdhesion statut = StatutAdhesion.EN_ATTENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traite_par_id")
    private Membre traitePar;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;

    @Column(name = "motif_decision", length = 2000)
    private String motifDecision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membre_cree_id")
    private Membre membreCree;
}
