package com.lias.gestion_laboratoire.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lias.gestion_laboratoire.models.enums.TypePublication;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entité JPA représentant une publication scientifique produite par
 * les membres du laboratoire de recherche LIAS.
 *
 * <p>
 * Une publication peut avoir plusieurs auteurs ({@link Membre}), et
 * un membre peut co-signer plusieurs publications — d'où la relation
 * {@link ManyToMany} bidirectionnelle gérée via la table intermédiaire
 * {@code publication_auteurs}.
 * </p>
 *
 * <p>
 * <strong>Règle de non-suppression :</strong> les publications ne sont
 * jamais supprimées physiquement. Le champ {@code estArchive} permet de
 * les retirer de l'affichage actif tout en conservant l'historique.
 * </p>
 *
 * @author LIAS Laboratory
 * @see Membre
 * @see TypePublication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "publications")
public class Publication {

    // =========================================================================
    // IDENTIFIANT
    // =========================================================================

    /** Identifiant unique auto-généré (clé primaire). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // INFORMATIONS DE LA PUBLICATION
    // =========================================================================

    /** Titre complet de la publication scientifique. */
    @Column(name = "titre", nullable = false, length = 255)
    private String titre;

    /**
     * Résumé (abstract) de la publication.
     * Utilise {@code TEXT} en base pour autoriser de longs contenus.
     */
    @Column(name = "resume", columnDefinition = "TEXT")
    private String resume;

    /** Date de publication officielle. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "date_publication", nullable = false)
    private LocalDate datePublication;

    /**
     * Lien DOI (Digital Object Identifier) de la publication.
     * Permet d'identifier et d'accéder de manière unique à l'article
     * (ex : {@code https://doi.org/10.1000/xyz123}).
     */
    @Column(name = "lien_doi", length = 300)
    private String lienDoi;

    // =========================================================================
    // TYPE DE PUBLICATION
    // =========================================================================

    /**
     * Type de la publication scientifique.
     *
     * @see TypePublication
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_publication", nullable = false, length = 20)
    private TypePublication type;

    // =========================================================================
    // RELATION MANY-TO-MANY — AUTEURS
    // =========================================================================

    /**
     * Ensemble des membres du laboratoire co-auteurs de cette publication.
     *
     * <p>
     * Côté propriétaire (owning side) de la relation bidirectionnelle
     * {@link ManyToMany}. La table de jointure {@code publication_auteurs}
     * est gérée depuis cette entité.
     * </p>
     *
     * <p>
     * {@code @JsonIgnoreProperties("publications")} coupe la récursion
     * infinie lors de la sérialisation JSON (Jackson).
     * </p>
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToMany
    @JoinTable(name = "publication_auteurs", joinColumns = @JoinColumn(name = "publication_id"), inverseJoinColumns = @JoinColumn(name = "membre_id"))
    @JsonIgnoreProperties("publications")
    private Set<Membre> auteurs = new HashSet<>();

    // =========================================================================
    // GESTION DE L'ARCHIVAGE (SOFT DELETE)
    // =========================================================================

    /**
     * Indicateur d'archivage de la publication.
     *
     * <p>
     * <strong>Règle métier :</strong> Les publications ne sont jamais
     * supprimées physiquement. Mettre ce champ à {@code true} archive la
     * publication, la retirant de l'affichage actif tout en conservant
     * l'intégralité de ses données en base.
     * </p>
     *
     * <p>
     * Valeur par défaut : {@code false} (publication visible à la création).
     * </p>
     */
    @Column(name = "est_archive", nullable = false)
    private boolean estArchive = false;

    // =========================================================================
    // AUDIT — TIMESTAMPS AUTOMATIQUES
    // =========================================================================

    /**
     * Date et heure de création de l'enregistrement.
     * Alimentée automatiquement par Hibernate lors du premier {@code persist}.
     * Non modifiable après création ({@code updatable = false}).
     */
    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    /**
     * Date et heure de la dernière mise à jour de l'enregistrement.
     * Mise à jour automatiquement par Hibernate à chaque {@code merge}.
     */
    @UpdateTimestamp
    @Column(name = "date_mise_a_jour", nullable = false)
    private LocalDateTime dateMiseAJour;
}
