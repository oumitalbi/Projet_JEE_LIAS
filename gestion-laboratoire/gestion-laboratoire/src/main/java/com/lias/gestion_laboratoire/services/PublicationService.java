package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.authorization.AuthorizationService;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.Publication;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import com.lias.gestion_laboratoire.repositories.PublicationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service métier pour la gestion des publications scientifiques
 * du laboratoire de recherche LIAS.
 *
 * <p>
 * Cette classe centralise toute la logique métier relative aux
 * publications : ajout avec validation des auteurs, consultation filtrée
 * par type, auteur, équipe et archivage logique (soft delete).
 * </p>
 *
 * <p>
 * <strong>Règle fondamentale :</strong> Aucune publication n'est jamais
 * supprimée physiquement de la base de données. La méthode
 * {@link #archiverPublication(Long)} se substitue à toute opération
 * de suppression.
 * </p>
 *
 * @author LIAS Laboratory
 * @see Publication
 * @see PublicationRepository
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PublicationService {

    /**
     * Repository des publications, injecté via le constructeur généré par Lombok.
     */
    private final PublicationRepository publicationRepository;

    /** Repository des membres, injecté via le constructeur généré par Lombok. */
    private final MembreRepository membreRepository;

    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;

    // =========================================================================
    // CRÉATION
    // =========================================================================

    /**
     * Ajoute une nouvelle publication scientifique au laboratoire LIAS
     * en y associant un ensemble d'auteurs identifiés par leurs IDs.
     *
     * <p>
     * <strong>Règle métier :</strong> Chaque identifiant de la collection
     * {@code auteursIds} doit correspondre à un membre existant en base.
     * Si un identifiant ne correspond à aucun membre, une
     * {@link EntityNotFoundException} est levée et la transaction est
     * annulée (rollback automatique grâce à {@code @Transactional}).
     * </p>
     *
     * @param publication la publication à persister
     * @param auteursIds  ensemble des identifiants des membres co-auteurs
     * @return la publication sauvegardée avec ses auteurs et son identifiant généré
     * @throws EntityNotFoundException si un des identifiants de membre n'existe pas
     */
    public Publication ajouterPublication(Publication publication, Set<Long> auteursIds) {
        if (publication.getAuteurs() == null) {
            publication.setAuteurs(new HashSet<>());
        }
        for (Long membreId : auteursIds) {
            Membre membre = membreRepository.findById(membreId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Aucun membre trouvé avec l'ID : " + membreId));
            publication.getAuteurs().add(membre);
        }
        Publication saved = publicationRepository.save(publication);

        // Notification envoyée à tous les co-auteurs (section 19)
        if (saved.getAuteurs() != null) {
            for (Membre auteur : saved.getAuteurs()) {
                notificationService.creerNotification(
                        auteur.getId(),
                        "Nouvelle publication scientifique",
                        "Vous avez été ajouté comme co-auteur de « " + saved.getTitre() + " ».",
                        com.lias.gestion_laboratoire.models.enums.TypeNotification.PUBLICATION,
                        "PUBLICATION",
                        saved.getId()
                );
            }
        }

        return saved;
    }

    // =========================================================================
    // CONSULTATION — PUBLICATIONS ACTIVES
    // =========================================================================

    /**
     * Récupère la liste de toutes les publications non archivées.
     *
     * <p>
     * Cette méthode constitue le point d'entrée principal pour
     * l'affichage des publications dans l'interface de gestion.
     * Elle exclut automatiquement les publications archivées
     * (soft-deleted).
     * </p>
     *
     * @return liste de toutes les publications actives du laboratoire
     */
    @Transactional(readOnly = true)
    public List<Publication> obtenirToutesLesPublications() {
        return publicationRepository.findByEstArchive(false);
    }

    // =========================================================================
    // FILTRAGE PAR AUTEUR
    // =========================================================================

    /**
     * Récupère les publications non archivées d'un auteur spécifique.
     *
     * <p>
     * <strong>Cas d'usage :</strong> afficher la liste des publications
     * sur la page de profil d'un membre du laboratoire LIAS.
     * </p>
     *
     * @param membreId l'identifiant du membre auteur
     * @return liste des publications non archivées de l'auteur spécifié
     */
    @Transactional(readOnly = true)
    public List<Publication> obtenirPublicationsParAuteur(Long membreId) {
        return publicationRepository.findByAuteurs_IdAndEstArchiveFalse(membreId);
    }

    // =========================================================================
    // FILTRAGE PAR ÉQUIPE
    // =========================================================================

    /**
     * Récupère les publications non archivées dont au moins un auteur
     * appartient à l'équipe de recherche spécifiée.
     *
     * <p>
     * <strong>Cas d'usage :</strong> afficher les publications produites
     * par une équipe spécifique (ISDIAC, SIMA, ILIAS, SDTIC) dans le
     * tableau de bord du laboratoire LIAS.
     * </p>
     *
     * @param equipe l'équipe de recherche ciblée
     * @return liste des publications non archivées de l'équipe spécifiée
     * @see EquipeLaboratoire
     */
    @Transactional(readOnly = true)
    public List<Publication> obtenirPublicationsParEquipe(EquipeLaboratoire equipe) {
        return publicationRepository.findByAuteurs_EquipeLaboratoireAndEstArchiveFalse(equipe);
    }

    // =========================================================================
    // SOFT DELETE — ARCHIVAGE
    // =========================================================================

    /**
     * Archive logiquement une publication du laboratoire (soft delete).
     *
     * <p>
     * <strong>Règle métier :</strong> Conformément à la politique de
     * non-suppression du laboratoire LIAS, cette méthode ne supprime
     * <em>jamais</em> physiquement un enregistrement. Elle passe le champ
     * {@code estArchive} à {@code true}, retirant la publication de
     * l'affichage actif tout en conservant l'intégralité de ses données
     * en base.
     * </p>
     *
     * @param id l'identifiant de la publication à archiver
     * @return la publication mise à jour avec {@code estArchive = true}
     * @throws EntityNotFoundException si aucune publication n'est trouvée pour l'ID
     *                                 fourni
     */
    public Publication archiverPublication(Long id) {
        Publication publication = publicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucune publication trouvée avec l'ID : " + id));

        authorizationService.verifierArchivagePublication(publication);
        publication.setEstArchive(true);
        Publication saved = publicationRepository.save(publication);

        // Notification d'archivage envoyée aux auteurs
        if (saved.getAuteurs() != null) {
            for (Membre auteur : saved.getAuteurs()) {
                notificationService.creerNotification(
                        auteur.getId(),
                        "Publication archivée",
                        "La publication « " + saved.getTitre() + " » a été archivée.",
                        com.lias.gestion_laboratoire.models.enums.TypeNotification.PUBLICATION,
                        "PUBLICATION",
                        saved.getId()
                );
            }
        }

        return saved;
    }

    /**
     * Récupère la liste de toutes les publications archivées.
     */
    @Transactional(readOnly = true)
    public List<Publication> obtenirPublicationsArchivees() {
        return publicationRepository.findByEstArchive(true);
    }

    /**
     * Désarchive logiquement une publication du laboratoire.
     */
    public Publication desarchiverPublication(Long id) {
        Publication publication = publicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucune publication trouvée avec l'ID : " + id));

        publication.setEstArchive(false);
        return publicationRepository.save(publication);
    }
}
