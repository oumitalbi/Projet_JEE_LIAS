package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.Message;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.TypeMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository Spring Data JPA pour l'entité {@link Message}.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // =========================================================================
    // MESSAGES DIRECTS — CONVERSATION ENTRE DEUX MEMBRES
    // =========================================================================

    /**
     * Récupère le fil complet des échanges directs entre deux membres (chronologique).
     */
    @Query("SELECT m FROM Message m WHERE m.typeMessage = 'DIRECT' AND m.archivee = false AND " +
           "((m.expediteur.id = :m1 AND m.destinataire.id = :m2) OR (m.expediteur.id = :m2 AND m.destinataire.id = :m1)) " +
           "ORDER BY m.dateEnvoi ASC")
    List<Message> findDiscussionDirecte(@Param("m1") Long m1, @Param("m2") Long m2);

    /**
     * Récupère tous les messages directs impliquant un membre (expéditeur ou destinataire).
     */
    @Query("SELECT m FROM Message m WHERE m.typeMessage = 'DIRECT' AND m.archivee = false AND " +
           "(m.expediteur.id = :membreId OR m.destinataire.id = :membreId) " +
           "ORDER BY m.dateEnvoi DESC")
    List<Message> findAllDirectMessagesForMember(@Param("membreId") Long membreId);

    /**
     * Compte les messages directs non lus reçus par un membre.
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.typeMessage = 'DIRECT' AND m.archivee = false AND " +
           "m.destinataire.id = :membreId AND m.lu = false")
    long countDirectNonLus(@Param("membreId") Long membreId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.typeMessage = 'EQUIPE' AND m.archivee = false " +
           "AND m.equipeDestinataire = :equipe AND m.expediteur.id <> :membreId " +
           "AND NOT EXISTS (SELECT ml.id FROM MessageLecture ml WHERE ml.message = m AND ml.lecteur.id = :membreId)")
    long countUnreadTeamForMember(@Param("membreId") Long membreId,
                                  @Param("equipe") EquipeLaboratoire equipe);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.typeMessage = 'EVENEMENT' AND m.archivee = false " +
           "AND m.expediteur.id <> :membreId " +
           "AND NOT EXISTS (SELECT ml.id FROM MessageLecture ml WHERE ml.message = m AND ml.lecteur.id = :membreId) " +
           "AND (:supervision = true OR :membre MEMBER OF m.evenement.organisateurs " +
           "OR :membre MEMBER OF m.evenement.participants)")
    long countUnreadEventsForMember(@Param("membreId") Long membreId,
                                    @Param("membre") Membre membre,
                                    @Param("supervision") boolean supervision);

    // =========================================================================
    // MESSAGES D'ÉQUIPE
    // =========================================================================

    @Query("SELECT m FROM Message m WHERE m.typeMessage = 'EQUIPE' AND m.archivee = false AND " +
           "m.equipeDestinataire = :equipe ORDER BY m.dateEnvoi ASC")
    List<Message> findMessagesEquipe(@Param("equipe") EquipeLaboratoire equipe);

    @Query("SELECT m FROM Message m WHERE m.typeMessage = 'EQUIPE' AND m.archivee = false AND " +
           "m.equipeDestinataire = :equipe")
    List<Message> findAllMessagesEquipe(@Param("equipe") EquipeLaboratoire equipe);

    // =========================================================================
    // MESSAGES D'ÉVÉNEMENT
    // =========================================================================

    @Query("SELECT m FROM Message m WHERE m.typeMessage = 'EVENEMENT' AND m.archivee = false AND " +
           "m.evenement.id = :evenementId ORDER BY m.dateEnvoi ASC")
    List<Message> findMessagesEvenement(@Param("evenementId") Long evenementId);

    @Query("SELECT m FROM Message m WHERE m.typeMessage = 'EVENEMENT' AND m.archivee = false AND " +
           "m.evenement.id = :evenementId")
    List<Message> findAllMessagesEvenement(@Param("evenementId") Long evenementId);

    // =========================================================================
    // MÉTHODES DE COMPATIBILITÉ
    // =========================================================================

    List<Message> findByDestinataire_IdOrderByDateEnvoiDesc(Long membreId);

    List<Message> findByDestinataire_IdAndArchiveeFalseOrderByDateEnvoiDesc(Long membreId);

    List<Message> findByEquipeDestinataireOrderByDateEnvoiDesc(EquipeLaboratoire equipe);

    List<Message> findByExpediteur_IdOrderByDateEnvoiDesc(Long membreId);

    List<Message> findByEvenement_IdOrderByDateEnvoiAsc(Long evenementId);
}
