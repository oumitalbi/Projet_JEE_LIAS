package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.Notification;
import com.lias.gestion_laboratoire.models.enums.RoleUtilisateur;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.models.enums.TypeNotification;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import com.lias.gestion_laboratoire.repositories.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Service métier pour la gestion des notifications du laboratoire LIAS.
 *
 * <p>Ce service garantit la sécurité et l'intégrité des notifications :
 * <ul>
 *   <li>Aucune notification n'est envoyée aux membres inactifs (RETRAITE, ANCIEN, compte inactif/gelé).</li>
 *   <li>Aucune notification interne n'est envoyée aux doctorants (section 2.2.C).</li>
 *   <li>Les opérations de consultation et de marquage sont protégées par le JWT du membre connecté.</li>
 *   <li>Conformément à la section 21, aucune suppression physique n'est autorisée (archivage logique).</li>
 * </ul>
 * </p>
 *
 * @author LIAS Laboratory
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MembreRepository membreRepository;

    // =========================================================================
    // CRÉATION DE NOTIFICATION (USAGE INTERNE PAR LES SERVICES DU LABO)
    // =========================================================================

    /**
     * Crée une nouvelle notification système avec type et cible pour la redirection.
     *
     * @param membreId  l'identifiant du membre concerné
     * @param titre     le titre court de l'alerte
     * @param contenu   le détail textuel
     * @param type      la catégorie fonctionnelle (TypeNotification)
     * @param cibleType le type d'entité liée ("EVENEMENT", "DOCUMENT", etc.)
     * @param cibleId   l'ID de l'entité liée
     * @return la notification sauvegardée, ou {@code null} si le membre est inéligible
     */
    public Notification creerNotification(Long membreId, String titre, String contenu,
                                         TypeNotification type, String cibleType, Long cibleId) {
        if (membreId == null) {
            return null;
        }

        Membre membre = membreRepository.findById(membreId).orElse(null);
        if (membre == null) {
            log.warn("Tentative de notification pour un membre introuvable id={}", membreId);
            return null;
        }

        // Règle 1 : Aucun envoi aux membres inactifs (RETRAITE, ANCIEN, désactivé, gelé)
        if (membre.getStatut() == Statut.ANCIEN
                || membre.getStatut() == Statut.RETRAITE
                || !membre.isCompteActif()
                || membre.isCompteGele()) {
            log.debug("Notification ignorée pour membre inactif id={}", membreId);
            return null;
        }

        // Règle 2 : Aucune notification interne aux doctorants (section 2.2.C)
        if (membre.getStatut() == Statut.DOCTORANT || membre.getRoleSysteme() == RoleUtilisateur.DOCTORANT) {
            log.debug("Notification ignorée pour doctorant id={}", membreId);
            return null;
        }

        Notification notification = new Notification();
        notification.setMembre(membre);
        notification.setTitre(titre);
        notification.setContenu(contenu);
        notification.setTypeNotification(type != null ? type : TypeNotification.SYSTEME);
        notification.setCibleType(cibleType);
        notification.setCibleId(cibleId);
        notification.setLue(false);
        notification.setArchivee(false);

        return notificationRepository.save(notification);
    }

    /**
     * Méthode de compatibilité pour la création de notification simple.
     */
    public Notification creerNotification(Long membreId, String titre, String contenu) {
        return creerNotification(membreId, titre, contenu, TypeNotification.SYSTEME, null, null);
    }

    // =========================================================================
    // CONSULTATION PAR LE MEMBRE CONNECTÉ (/api/notifications/moi)
    // =========================================================================

    /**
     * Récupère toutes les notifications actives (non archivées) du membre connecté.
     */
    @Transactional(readOnly = true)
    public List<Notification> obtenirMesNotifications(Membre membre) {
        return notificationRepository.findByMembre_IdAndArchiveeFalseOrderByDateCreationDesc(membre.getId());
    }

    /**
     * Récupère les notifications non lues et non archivées du membre connecté.
     */
    @Transactional(readOnly = true)
    public List<Notification> obtenirMesNotificationsNonLues(Membre membre) {
        return notificationRepository.findByMembre_IdAndLueFalseAndArchiveeFalseOrderByDateCreationDesc(membre.getId());
    }

    /**
     * Compte les notifications non lues du membre connecté pour le badge.
     */
    @Transactional(readOnly = true)
    public long compterMesNotificationsNonLues(Membre membre) {
        return notificationRepository.countByMembre_IdAndLueFalseAndArchiveeFalse(membre.getId());
    }

    // =========================================================================
    // GESTION DU STATUT DE LECTURE ET ARCHIVAGE LOGIQUE
    // =========================================================================

    /**
     * Marque toutes les notifications non lues du membre connecté comme lues.
     */
    public void marquerToutesMesNotificationsCommeLues(Membre membre) {
        List<Notification> nonLues = notificationRepository
                .findByMembre_IdAndLueFalseAndArchiveeFalseOrderByDateCreationDesc(membre.getId());
        nonLues.forEach(n -> n.setLue(true));
        notificationRepository.saveAll(nonLues);
    }

    /**
     * Marque une notification spécifique comme lue en vérifiant que le membre connecté en est le destinataire.
     */
    public Notification marquerNotificationCommeLue(Long notificationId, Membre membre) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification introuvable : " + notificationId));

        if (!notification.getMembre().getId().equals(membre.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous n'êtes pas autorisé à modifier cette notification.");
        }

        notification.setLue(true);
        return notificationRepository.save(notification);
    }

    /**
     * Archive logiquement une notification (soft delete) en vérifiant l'appartenance.
     */
    public Notification archiverNotification(Long notificationId, Membre membre) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification introuvable : " + notificationId));

        if (!notification.getMembre().getId().equals(membre.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous n'êtes pas autorisé à archiver cette notification.");
        }

        notification.setArchivee(true);
        return notificationRepository.save(notification);
    }

    // =========================================================================
    // MÉTHODES DE COMPATIBILITÉ
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Notification> obtenirNotificationsNonLues(Long membreId) {
        return notificationRepository.findByMembre_IdAndLueFalseAndArchiveeFalseOrderByDateCreationDesc(membreId);
    }

    @Transactional(readOnly = true)
    public List<Notification> obtenirToutesNotifications(Long membreId) {
        return notificationRepository.findByMembre_IdAndArchiveeFalseOrderByDateCreationDesc(membreId);
    }

    public void marquerToutesCommeLues(Long membreId) {
        List<Notification> nonLues = notificationRepository
                .findByMembre_IdAndLueFalseAndArchiveeFalseOrderByDateCreationDesc(membreId);
        nonLues.forEach(n -> n.setLue(true));
        notificationRepository.saveAll(nonLues);
    }
}
