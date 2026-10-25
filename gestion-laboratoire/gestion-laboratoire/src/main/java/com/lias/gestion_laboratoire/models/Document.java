package com.lias.gestion_laboratoire.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lias.gestion_laboratoire.models.enums.TypeDocument;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entité JPA représentant un document archivé dans le système
 * de gestion du laboratoire de recherche LIAS.
 *
 * <p>Un document peut être lié à un {@link Evenement} spécifique
 * (programme d'une conférence, rapport de séminaire…) ou exister
 * de manière indépendante en tant que document administratif général.</p>
 *
 * <p>Les métadonnées du fichier (nom, chemin de stockage, type MIME, taille)
 * sont conservées en base de données tandis que le fichier physique est
 * stocké sur le système de fichiers du serveur ou un service de stockage
 * distant.</p>
 *
 * @author LIAS Laboratory
 * @see Evenement
 * @see TypeDocument
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents")
public class Document {

    // =========================================================================
    // IDENTIFIANT
    // =========================================================================

    /** Identifiant unique auto-généré (clé primaire). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // MÉTADONNÉES DU FICHIER
    // =========================================================================

    /**
     * Nom d'origine du fichier tel qu'il a été uploadé par l'utilisateur.
     * Exemple : {@code programme_conf_2026.pdf}.
     */
    @Column(name = "nom_fichier", nullable = false, length = 255)
    private String nomFichier;

    /**
     * Chemin physique ou URL de stockage du fichier sur le serveur.
     *
     * <p>Peut pointer vers un répertoire local du serveur d'application
     * (ex : {@code /data/uploads/documents/xxx.pdf}) ou vers un service
     * de stockage distant (S3, MinIO, CDN…).</p>
     */
    @Column(name = "chemin_stockage", nullable = false, length = 500)
    private String cheminStockage;

    /**
     * Type MIME du fichier permettant d'identifier son format.
     * Exemples : {@code application/pdf}, {@code image/png},
     * {@code application/vnd.openxmlformats-officedocument.wordprocessingml.document}.
     */
    @Column(name = "content_type", length = 150)
    private String contentType;

    /**
     * Taille du fichier en octets.
     *
     * <p>Permet d'afficher la taille dans l'interface utilisateur
     * et d'appliquer d'éventuelles règles de validation
     * (taille maximale autorisée).</p>
     */
    @Column(name = "taille")
    private Long taille;

    // =========================================================================
    // TYPE DE DOCUMENT
    // =========================================================================

    /**
     * Catégorie du document archivé.
     * Stockée sous forme de chaîne de caractères en base ({@code EnumType.STRING})
     * pour la lisibilité des données et la robustesse face aux refactorisations.
     *
     * @see TypeDocument
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", nullable = false, length = 50)
    private TypeDocument typeDocument;

    // =========================================================================
    // RELATION MANY-TO-ONE — ÉVÉNEMENT
    // =========================================================================

    /**
     * Événement scientifique auquel ce document est rattaché.
     *
     * <p>La relation est <strong>facultative</strong> ({@code optional = true}) :
     * un document de type {@link TypeDocument#DOCUMENT_ADMINISTRATIF} peut
     * exister indépendamment de tout événement, tandis qu'un programme ou
     * une attestation sera généralement lié à un événement précis.</p>
     *
     * <p>Le chargement est {@code LAZY} pour éviter les requêtes superflues
     * lors de la consultation de la liste des documents.</p>
     *
     * <p>{@code @JsonIgnoreProperties("documents")} coupe la récursion
     * infinie lors de la sérialisation JSON (Jackson).</p>
     *
     * @see Evenement#documents
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evenement_id")
    @JsonIgnoreProperties("documents")
    private Evenement evenement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depose_par_id")
    @JsonIgnoreProperties({
            "publications", "evenementsOrganises", "evenementsParticipes", "documents",
            "motDePasse", "notifications", "equipes"
    })
    private Membre deposePar;

    @Column(name = "est_archive", nullable = false, columnDefinition = "boolean default false")
    private boolean estArchive;

    @Column(name = "date_archivage")
    private LocalDateTime dateArchivage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_par_id")
    @JsonIgnoreProperties({
            "publications", "evenementsOrganises", "evenementsParticipes", "documents",
            "motDePasse", "notifications", "equipes"
    })
    private Membre archivePar;

    // =========================================================================
    // AUDIT — TIMESTAMP AUTOMATIQUE
    // =========================================================================

    /**
     * Date et heure de l'upload du document.
     * Alimentée automatiquement par Hibernate lors du premier {@code persist}.
     * Non modifiable après création ({@code updatable = false}).
     */
    @CreationTimestamp
    @Column(name = "date_upload", nullable = false, updatable = false)
    private LocalDateTime dateUpload;
}
