package com.lias.gestion_laboratoire.models;

import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.Role;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "historique_roles")
@Data
@NoArgsConstructor
public class HistoriqueRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_role", length = 30)
    private Role ancienRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "nouveau_role", nullable = false, length = 30)
    private Role nouveauRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancienne_equipe", length = 20)
    private EquipeLaboratoire ancienneEquipe;

    @Enumerated(EnumType.STRING)
    @Column(name = "nouvelle_equipe", length = 20)
    private EquipeLaboratoire nouvelleEquipe;

    @Column(name = "motif", length = 1000)
    private String motif;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifie_par_id")
    private Membre modifiePar;

    @Column(name = "date_changement", nullable = false, updatable = false)
    private LocalDateTime dateChangement;

    @PrePersist
    void initialiserDate() {
        if (dateChangement == null) dateChangement = LocalDateTime.now();
    }
}
