package com.lias.gestion_laboratoire.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "arrivages_materiel")
@Data
@NoArgsConstructor
public class ArrivageMateriel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materiel_id", nullable = false)
    private Materiel materiel;

    @Column(nullable = false)
    private Integer quantite;

    @Column(name = "date_arrivage", nullable = false)
    private LocalDate dateArrivage;

    @Column(length = 200)
    private String fournisseur;

    @Column(name = "bon_livraison", length = 200)
    private String bonLivraison;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enregistre_par_id", nullable = false)
    private Membre enregistrePar;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    void initialiserDateCreation() {
        if (dateCreation == null) dateCreation = LocalDateTime.now();
    }
}
