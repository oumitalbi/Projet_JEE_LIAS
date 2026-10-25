package com.lias.gestion_laboratoire.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lias.gestion_laboratoire.models.enums.TypeNotification;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entité JPA représentant une notification destinée à un membre
 * du laboratoire de recherche LIAS.
 *
 * <p>Les notifications sont des alertes système générées automatiquement
 * pour informer un membre d'un événement important (ex : demande de matériel
 * validée, nouveau message reçu, changement de statut).</p>
 *
 * <p>Chaque notification possède un indicateur de lecture ({@code lue})
 * permettant au membre de consulter uniquement les alertes non lues.</p>
 *
 * <p>Conformément à la section 21 du cahier des charges, aucune notification
 * n'est supprimée physiquement. Le champ {@code archivee} permet un masquage
 * logique.</p>
 *
 * @author LIAS Laboratory
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

    // =========================================================================
    // IDENTIFIANT
    // =========================================================================

    /** Identifiant unique auto-généré (clé primaire). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // RELATION — MEMBRE CONCERNÉ
    // =========================================================================

    /**
     * Membre destinataire de cette notification.
     * Obligatoire : chaque notification cible un membre précis.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "membre_id", nullable = false)
    @JsonIgnoreProperties({"publications", "evenements", "materielsEnPossession",
            "mesDemandes", "reunionsParticipes", "historiqueMandats", "motDePasse"})
    private Membre membre;

    // =========================================================================
    // CONTENU
    // =========================================================================

    /** Titre court de la notification (ex : « Demande validée »). */
    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    /** Description détaillée de la notification. */
    @Column(name = "contenu", columnDefinition = "TEXT")
    private String contenu;

    // =========================================================================
    // CLASSIFICATION ET NAVIGATION
    // =========================================================================

    /**
     * Type de la notification pour le filtrage et l'affichage d'icône.
     * Stocké en VARCHAR via EnumType.STRING.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_notification", length = 30)
    private TypeNotification typeNotification;

    /**
     * Type de l'entité cible pour la redirection au clic
     * (ex : "EVENEMENT", "DOCUMENT", "REUNION", "CONVENTION").
     */
    @Column(name = "cible_type", length = 30)
    private String cibleType;

    /**
     * Identifiant de l'entité cible pour la redirection au clic.
     */
    @Column(name = "cible_id")
    private Long cibleId;

    // =========================================================================
    // MÉTADONNÉES
    // =========================================================================

    /**
     * Indicateur de lecture de la notification.
     * {@code false} par défaut (non lue à la création).
     */
    @Column(name = "lue", nullable = false)
    private boolean lue = false;

    /**
     * Indicateur d'archivage logique (soft delete).
     * Conformément à la section 21, aucune suppression définitive.
     */
    @Column(name = "archivee", nullable = false)
    private boolean archivee = false;

    /**
     * Date et heure de création de la notification.
     * Alimentée automatiquement par Hibernate lors de la création.
     */
    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
