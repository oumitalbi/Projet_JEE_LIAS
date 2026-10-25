package com.lias.gestion_laboratoire.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.TypeMessage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entité JPA représentant un message échangé entre membres du laboratoire LIAS.
 *
 * <p>Un message peut être :
 * <ul>
 *   <li><strong>DIRECT</strong> : envoyé d'un expéditeur à un destinataire précis</li>
 *   <li><strong>EQUIPE</strong> : envoyé à tous les membres d'une équipe de recherche</li>
 *   <li><strong>EVENEMENT</strong> : discussion dans le contexte d'un événement scientifique</li>
 * </ul>
 * </p>
 *
 * @author LIAS Laboratory
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "messages")
public class Message {

    // =========================================================================
    // IDENTIFIANT
    // =========================================================================

    /** Identifiant unique auto-généré (clé primaire). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // TYPE DE MESSAGE
    // =========================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "type_message", nullable = false, length = 20)
    private TypeMessage typeMessage = TypeMessage.DIRECT;

    // =========================================================================
    // RELATIONS — EXPÉDITEUR / DESTINATAIRE
    // =========================================================================

    /**
     * Membre expéditeur du message.
     * Obligatoire : chaque message a un auteur identifié.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "expediteur_id", nullable = false)
    @JsonIgnoreProperties({"publications", "evenements", "materielsEnPossession",
            "mesDemandes", "reunionsParticipes", "historiqueMandats", "motDePasse"})
    private Membre expediteur;

    /**
     * Membre destinataire du message (pour les messages directs).
     * {@code null} si le message est destiné à une équipe entière ou un événement.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destinataire_id")
    @JsonIgnoreProperties({"publications", "evenements", "materielsEnPossession",
            "mesDemandes", "reunionsParticipes", "historiqueMandats", "motDePasse"})
    private Membre destinataire;

    // =========================================================================
    // RELATION — ÉQUIPE DESTINATAIRE
    // =========================================================================

    /**
     * Équipe de recherche destinataire du message (pour les messages d'équipe).
     * {@code null} si le message est un message direct ou lié à un événement.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "equipe_destinataire", length = 10)
    private EquipeLaboratoire equipeDestinataire;

    // =========================================================================
    // RELATION — ÉVÉNEMENT LIÉ
    // =========================================================================

    /**
     * Événement scientifique auquel ce message est rattaché (discussion liée).
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "evenement_id")
    @JsonIgnoreProperties({"participants", "documents", "organisateurs"})
    private Evenement evenement;

    // =========================================================================
    // CONTENU
    // =========================================================================

    /** Contenu textuel du message. */
    @Column(name = "contenu", nullable = false, columnDefinition = "TEXT")
    private String contenu;

    // =========================================================================
    // MÉTADONNÉES
    // =========================================================================

    /**
     * Indicateur de lecture du message (utilisé principalement pour les messages directs).
     */
    @Column(name = "lu", nullable = false)
    private boolean lu = false;

    /**
     * Indicateur d'archivage logique (soft delete, section 21).
     */
    @Column(name = "archivee", nullable = false)
    private boolean archivee = false;

    /**
     * Date et heure d'envoi du message.
     */
    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "date_envoi", nullable = false, updatable = false)
    private LocalDateTime dateEnvoi;
}
