package com.lias.gestion_laboratoire.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lias.gestion_laboratoire.models.enums.TypePartenaire;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Entité JPA représentant un organisme partenaire collaborant avec le laboratoire LIAS.
 *
 * <p>Un partenaire (entreprise, université, institution) peut signer une ou
 * plusieurs conventions de recherche ou de collaboration avec le laboratoire.</p>
 */
@Entity
@Table(name = "partenaires")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Partenaire {

    /** Identifiant unique du partenaire. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom officiel de l'organisme partenaire. */
    @Column(nullable = false)
    private String nom;

    /** Le type d'entité du partenaire (académique, industriel, institutionnel). */
    @Enumerated(EnumType.STRING)
    private TypePartenaire type;

    /** Pays où se situe le siège du partenaire. */
    @Column(name = "pays")
    private String pays;

    /** Adresse e-mail du contact principal chez le partenaire. */
    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "archive")
    private Boolean archive = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_par_id")
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Membre archivePar;

    @Column(name = "date_archivage")
    private LocalDateTime dateArchivage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaure_par_id")
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Membre restaurePar;

    @Column(name = "date_restauration")
    private LocalDateTime dateRestauration;

    // =========================================================================
    // RELATIONS
    // =========================================================================

    /**
     * Liste des conventions signées entre ce partenaire et le laboratoire.
     * 
     * <p>Côté inverse de la relation @ManyToOne définie dans Convention.
     * Les partenaires sont archivés logiquement et ne suppriment pas leurs conventions.</p>
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "partenaire")
    @JsonIgnore // Évite la boucle infinie de sérialisation JSON
    private List<Convention> conventions = new ArrayList<>();

    public boolean isArchive() {
        return Boolean.TRUE.equals(archive);
    }

    @PrePersist
    void initialiserArchivage() {
        if (archive == null) archive = false;
    }
}
