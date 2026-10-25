package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.Evenement;
import com.lias.gestion_laboratoire.models.enums.TypeEvenement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository Spring Data JPA pour l'entité {@link Evenement}.
 *
 * <p>Ce repository fournit l'ensemble des opérations CRUD standard via
 * {@link JpaRepository} ainsi que des méthodes de recherche personnalisées
 * adaptées aux règles métier du laboratoire LIAS.</p>
 *
 * <p><strong>Règle métier de non-suppression :</strong> Les événements ne
 * sont jamais supprimés physiquement. Toutes les requêtes orientées
 * « affichage » filtrent par {@code estAnnule = false} afin de ne
 * présenter que les événements actifs.</p>
 *
 * @author LIAS Laboratory
 * @see Evenement
 */
@Repository
public interface EvenementRepository extends JpaRepository<Evenement, Long> {

    boolean existsByTitre(String titre);

    // =========================================================================
    // 1. ÉVÉNEMENTS ACTIFS (SOFT DELETE)
    // =========================================================================

    /**
     * Récupère tous les événements non annulés, triés chronologiquement
     * par date de début croissante.
     *
     * <p>Cette méthode est le point d'entrée principal pour l'affichage
     * du calendrier des événements : elle exclut automatiquement les
     * événements annulés (soft-deleted) conformément à la règle de
     * non-suppression du laboratoire LIAS.</p>
     *
     * @return liste de tous les événements actifs, triés par {@code dateDebut} ASC
     */
    @EntityGraph(attributePaths = {"organisateurs", "participants"})
    List<Evenement> findByEstAnnuleFalseOrderByDateDebutAsc();

    @EntityGraph(attributePaths = {"organisateurs", "participants"})
    List<Evenement> findByEstAnnuleTrueOrderByDateDebutDesc();

    @EntityGraph(attributePaths = {"organisateurs", "participants"})
    List<Evenement> findByOrganisateurs_IdAndEstAnnuleTrueOrderByDateDebutDesc(Long membreId);

    @EntityGraph(attributePaths = {"organisateurs", "participants"})
    java.util.Optional<Evenement> findWithParticipantsAndOrganisateursById(Long id);

    // =========================================================================
    // 2. ÉVÉNEMENTS À VENIR
    // =========================================================================

    /**
     * Récupère les événements à venir (non annulés) dont la date de début
     * est postérieure à une date de référence donnée, triés par date de début
     * croissante.
     *
     * <p><strong>Cas d'usage :</strong> afficher les prochains événements du
     * laboratoire sur la page d'accueil ou dans le widget « agenda ».</p>
     *
     * @param date la date de référence (généralement {@code LocalDateTime.now()})
     * @return liste des événements à venir, triés par {@code dateDebut} ASC
     */
    @EntityGraph(attributePaths = {"organisateurs", "participants"})
    List<Evenement> findByDateDebutAfterAndEstAnnuleFalseOrderByDateDebutAsc(LocalDateTime date);

    // =========================================================================
    // 3. FILTRAGE PAR TYPE D'ÉVÉNEMENT
    // =========================================================================

    /**
     * Récupère les événements non annulés d'un type spécifique, triés
     * par date de début croissante.
     *
     * <p><strong>Cas d'usage :</strong> filtrer les événements par catégorie
     * (CONFERENCE, SEMINAIRE, SOUTENANCE_THESE, WORKSHOP) sur la page
     * de consultation du calendrier scientifique du laboratoire LIAS.</p>
     *
     * @param type le type d'événement recherché
     * @return liste des événements non annulés du type demandé, triés par date
     * @see TypeEvenement
     */
    @EntityGraph(attributePaths = {"organisateurs", "participants"})
    List<Evenement> findByTypeAndEstAnnuleFalseOrderByDateDebutAsc(TypeEvenement type);

    // =========================================================================
    // 4. FILTRAGE PAR PARTICIPANT
    // =========================================================================

    /**
     * Récupère les événements non annulés auxquels participe un membre
     * identifié par son identifiant.
     *
     * <p>Utilise la navigation automatique de Spring Data JPA à travers
     * la relation {@code participants} pour filtrer par l'identifiant du
     * {@link com.lias.gestion_laboratoire.models.Membre}.</p>
     *
     * <p><strong>Cas d'usage :</strong> afficher le programme des événements
     * d'un membre spécifique sur sa page de profil dans le laboratoire LIAS.</p>
     *
     * @param membreId l'identifiant du membre participant
     * @return liste des événements non annulés auxquels le membre participe
     */
    @EntityGraph(attributePaths = {"organisateurs", "participants"})
    List<Evenement> findByParticipants_IdAndEstAnnuleFalse(Long membreId);
    // =========================================================================
    // 5. RECHERCHE GLOBALE (MODULE 18)
    // =========================================================================

    /**
     * Recherche globale par mot-clé à travers les champs texte principaux.
     */
    List<Evenement> findByTitreContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrLieuContainingIgnoreCase(String titre, String description, String lieu);

    // =========================================================================
    // 6. FILTRAGE PAR PÉRIODE (CALENDRIER)
    // =========================================================================

    /**
     * Récupère les événements non annulés dont la date de début est comprise entre
     * deux dates (par exemple, pour afficher les événements d'un mois donné).
     *
     * @param start date de début de la période
     * @param end   date de fin de la période
     * @return liste des événements de la période, triés par date
     */
    @EntityGraph(attributePaths = {"organisateurs", "participants"})
    List<Evenement> findByDateDebutBetweenAndEstAnnuleFalseOrderByDateDebutAsc(LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"organisateurs", "participants"})
    List<Evenement> findByDateDebutBetweenAndEstAnnuleTrueOrderByDateDebutAsc(LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"organisateurs", "participants"})
    @Query("select e from Evenement e where e.estAnnule = false and e.dateDebut <= :end and e.dateFin >= :start order by e.dateDebut")
    List<Evenement> findActiveIntersectingPeriod(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    @EntityGraph(attributePaths = {"organisateurs", "participants"})
    @Query("select e from Evenement e where e.estAnnule = true and e.dateDebut <= :end and e.dateFin >= :start order by e.dateDebut")
    List<Evenement> findCancelledIntersectingPeriod(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);
}
