package com.lias.gestion_laboratoire.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "historique_modifications_profil")
@Data
@NoArgsConstructor
public class HistoriqueModificationProfil {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effectue_par_id")
    private Membre effectuePar;

    @Column(name = "date_action", nullable = false, updatable = false)
    private LocalDateTime dateAction;

    @PrePersist
    void initialiserDate() {
        if (dateAction == null) dateAction = LocalDateTime.now();
    }
}
