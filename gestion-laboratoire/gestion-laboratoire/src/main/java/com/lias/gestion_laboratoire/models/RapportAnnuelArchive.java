package com.lias.gestion_laboratoire.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "rapports_annuels")
@Getter
@Setter
@NoArgsConstructor
public class RapportAnnuelArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int annee;

    @Column(nullable = false)
    private int version;

    @Column(name = "date_generation", nullable = false, updatable = false)
    private LocalDateTime dateGeneration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genere_par_id", nullable = false)
    private Membre generePar;

    @Column(name = "donnees_json", nullable = false, columnDefinition = "LONGTEXT")
    private String donneesJson;

    @PrePersist
    void initialiserDateGeneration() {
        if (dateGeneration == null) {
            dateGeneration = LocalDateTime.now();
        }
    }
}
