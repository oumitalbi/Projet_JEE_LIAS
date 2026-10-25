package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.Reunion;
import com.lias.gestion_laboratoire.models.enums.StatutReunion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité {@link Reunion}.
 *
 * <p>Fournit les opérations CRUD standard et des méthodes de recherche
 * adaptées à la gestion du calendrier des réunions du laboratoire.</p>
 */
@Repository
public interface ReunionRepository extends JpaRepository<Reunion, Long> {

    /**
     * Récupère toutes les réunions ayant un statut spécifique.
     *
     * @param statut le statut recherché (PLANIFIEE, EN_COURS, TERMINEE, ANNULEE)
     * @return la liste des réunions correspondantes
     */
    List<Reunion> findByStatut(StatutReunion statut);

    /**
     * Recherche les réunions planifiées dans une période de temps définie.
     *
     * <p>Cas d'usage : afficher l'agenda de la semaine ou du mois en cours.</p>
     *
     * @param debut la date et heure de début de la période
     * @param fin   la date et heure de fin de la période
     * @return la liste des réunions tenues dans cet intervalle
     */
    List<Reunion> findByDateReunionBetween(LocalDateTime debut, LocalDateTime fin);

    @EntityGraph(attributePaths = {"organisateur", "participants"})
    @Query("select r from Reunion r where r.archivee = false and r.dateReunion <= :fin " +
            "and (r.dateFin is null or r.dateFin >= :debut) order by r.dateReunion")
    List<Reunion> findCalendarItemsIntersectingPeriod(@Param("debut") LocalDateTime debut,
                                                       @Param("fin") LocalDateTime fin);

    /**
     * Récupère toutes les réunions auxquelles un membre spécifique a participé
     * ou est convié.
     *
     * <p>Utilise la navigation automatique de Spring Data JPA sur la collection
     * {@code participants}.</p>
     *
     * @param membreId l'identifiant du membre concerné
     * @return la liste des réunions de ce membre
     */
    List<Reunion> findByParticipants_Id(Long membreId);

    @Query("select distinct r from Reunion r left join r.participants p where p.id = :membreId or r.organisateur.id = :membreId")
    List<Reunion> findByParticipantOrOrganisateur(@Param("membreId") Long membreId);

    List<Reunion> findByArchiveeFalseOrderByDateReunionAsc();

    List<Reunion> findByArchiveeTrueOrderByDateReunionDesc();

    Optional<Reunion> findByIdAndParticipants_Id(Long id, Long membreId);

    boolean existsByTitre(String titre);
}
