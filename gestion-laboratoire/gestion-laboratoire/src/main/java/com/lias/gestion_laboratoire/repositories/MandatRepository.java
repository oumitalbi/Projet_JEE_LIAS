package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.Mandat;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository Spring Data JPA pour l'entité {@link Mandat}.
 *
 * <p>Fournit les opérations CRUD de base et les requêtes spécifiques pour 
 * gérer l'historique et l'état actuel des responsabilités au sein du laboratoire.</p>
 */
@Repository
public interface MandatRepository extends JpaRepository<Mandat, Long> {

    /**
     * Recherche tous les mandats actuellement en cours.
     * Un mandat est considéré "en cours" si sa date de fin est non renseignée (nulle).
     *
     * @return la liste des mandats actifs
     */
    List<Mandat> findByDateFinIsNull();

    /**
     * Recherche l'historique complet des mandats (passés et présents) d'un membre.
     * Les résultats sont triés du plus récent au plus ancien (basé sur la date de début).
     *
     * @param membreId l'identifiant du membre (titulaire)
     * @return la liste chronologique des mandats du membre
     */
    List<Mandat> findByTitulaire_IdOrderByDateDebutDesc(Long membreId);

    /**
     * Recherche le ou les mandats actuellement actifs pour un rôle ou une responsabilité
     * spécifique (ex: trouver l'actuel directeur du laboratoire).
     *
     * @param responsabilite le rôle ciblé
     * @return la liste des mandats actifs correspondants
     */
    List<Mandat> findByResponsabiliteAndDateFinIsNull(Role responsabilite);

    @Query("select m from Mandat m where m.dateDebut <= :date "
            + "and (m.dateFin is null or m.dateFin >= :date) order by m.dateDebut desc")
    List<Mandat> findMandatsValidesAu(@Param("date") java.time.LocalDate date);

    @Query("select m from Mandat m where m.dateDebut <= :date "
            + "and (m.dateFin is null or m.dateFin >= :date) "
            + "and m.responsabilite = :role order by m.dateDebut desc")
    List<Mandat> findMandatsRoleValidesAu(@Param("role") Role role, @Param("date") java.time.LocalDate date);

    @Query("select m from Mandat m where m.titulaire.id = :membreId and m.dateDebut <= :date "
            + "and (m.dateFin is null or m.dateFin >= :date) order by m.dateDebut desc")
    List<Mandat> findMandatsMembreValidesAu(@Param("membreId") Long membreId,
                                            @Param("date") java.time.LocalDate date);

    List<Mandat> findByResponsabiliteAndEquipeAndDateFinIsNull(Role role, EquipeLaboratoire equipe);

    @Query("select m from Mandat m where m.dateDebut <= :fin "
            + "and (m.dateFin is null or m.dateFin >= :debut)")
    List<Mandat> findIntersectingPeriod(@Param("debut") java.time.LocalDate debut,
                                        @Param("fin") java.time.LocalDate fin);

}
