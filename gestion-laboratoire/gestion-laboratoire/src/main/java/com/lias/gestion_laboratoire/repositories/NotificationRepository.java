package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository Spring Data JPA pour l'entité {@link Notification}.
 *
 * <p>Fournit les opérations de recherche pour les notifications d'un membre,
 * en tenant compte de l'indicateur d'archivage logique.</p>
 *
 * @author LIAS Laboratory
 * @see Notification
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // =========================================================================
    // REQUÊTES PRINCIPALES (EXCLUANT LES ARCHIVÉES)
    // =========================================================================

    /**
     * Récupère toutes les notifications actives (non archivées) d'un membre.
     */
    List<Notification> findByMembre_IdAndArchiveeFalseOrderByDateCreationDesc(Long membreId);

    /**
     * Récupère les notifications non lues et non archivées d'un membre.
     */
    List<Notification> findByMembre_IdAndLueFalseAndArchiveeFalseOrderByDateCreationDesc(Long membreId);

    /**
     * Compte le nombre de notifications non lues et non archivées pour un membre (pour le badge).
     */
    long countByMembre_IdAndLueFalseAndArchiveeFalse(Long membreId);

    // =========================================================================
    // MÉTHODES DE COMPATIBILITÉ
    // =========================================================================

    List<Notification> findByMembre_IdAndLueFalseOrderByDateCreationDesc(Long membreId);

    List<Notification> findByMembre_IdOrderByDateCreationDesc(Long membreId);
}
