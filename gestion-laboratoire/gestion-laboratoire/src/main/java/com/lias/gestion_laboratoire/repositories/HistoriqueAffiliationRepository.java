package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.HistoriqueAffiliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité {@link HistoriqueAffiliation}.
 *
 * <p>Fournit les opérations CRUD standard ainsi que des méthodes
 * pour consulter l'historique complet des affiliations d'un membre
 * et identifier la période d'affiliation en cours.</p>
 *
 * @author LIAS Laboratory
 * @see HistoriqueAffiliation
 */
@Repository
public interface HistoriqueAffiliationRepository
        extends JpaRepository<HistoriqueAffiliation, Long> {

    // =========================================================================
    // HISTORIQUE COMPLET D'UN MEMBRE (TRIÉ)
    // =========================================================================

    /**
     * Récupère l'historique complet des affiliations d'un membre,
     * trié de la plus récente à la plus ancienne.
     *
     * @param membreId l'identifiant du membre
     * @return liste des périodes d'affiliation du membre
     */
    List<HistoriqueAffiliation> findByMembre_IdOrderByDateDebutDesc(Long membreId);

    // =========================================================================
    // PÉRIODE D'AFFILIATION EN COURS
    // =========================================================================

    /**
     * Récupère la période d'affiliation en cours (dateFin = null)
     * d'un membre. Il ne devrait y en avoir qu'une seule à tout moment.
     *
     * @param membreId l'identifiant du membre
     * @return la période d'affiliation active, si elle existe
     */
    Optional<HistoriqueAffiliation> findByMembre_IdAndDateFinIsNull(Long membreId);

    @Query("select h from HistoriqueAffiliation h where h.dateDebut between :debut and :fin "
            + "or h.dateFin between :debut and :fin")
    List<HistoriqueAffiliation> findChangedWithinPeriod(@Param("debut") LocalDate debut,
                                                        @Param("fin") LocalDate fin);
}
