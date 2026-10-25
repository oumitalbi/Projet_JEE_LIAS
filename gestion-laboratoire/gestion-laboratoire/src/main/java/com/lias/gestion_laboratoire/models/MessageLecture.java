package com.lias.gestion_laboratoire.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entité de suivi de lecture par membre pour les messages de groupe
 * (messages d'équipe et discussions d'événements scientifiques).
 *
 * <p>Permet à chaque membre d'avoir son propre statut de lecture
 * pour les messages collectifs, évitant ainsi qu'une consultation
 * par un membre ne marque le message comme lu pour tout le monde.</p>
 *
 * @author LIAS Laboratory
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "messages_lectures", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"message_id", "lecteur_id"})
})
public class MessageLecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecteur_id", nullable = false)
    @JsonIgnoreProperties({"publications", "evenements", "materielsEnPossession",
            "mesDemandes", "reunionsParticipes", "historiqueMandats", "motDePasse"})
    private Membre lecteur;

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "date_lecture", nullable = false, updatable = false)
    private LocalDateTime dateLecture;

    public MessageLecture(Message message, Membre lecteur) {
        this.message = message;
        this.lecteur = lecteur;
    }
}
