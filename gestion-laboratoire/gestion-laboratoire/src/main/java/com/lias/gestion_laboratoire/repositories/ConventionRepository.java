package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.Convention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository Spring Data JPA pour l'entité {@link Convention}.
 *
 * <p>Fournit les opérations CRUD standard et des méthodes de recherche
 * dédiées au suivi des accords et collaborations du laboratoire.</p>
 */
@Repository
public interface ConventionRepository extends JpaRepository<Convention, Long> {

    /**
     * Récupère toutes les conventions liées à un partenaire spécifique.
     *
     * @param partenaireId l'identifiant du partenaire
     * @return la liste des conventions correspondantes
     */
    List<Convention> findByPartenaireId(Long partenaireId);

    /**
     * Recherche toutes les conventions actuellement actives.
     *
     * <p>Une convention est considérée comme active si sa date d'expiration
     * est strictement postérieure à la date de référence fournie (généralement
     * la date du jour).</p>
     *
     * @param date la date de référence (ex: LocalDate.now())
     * @return la liste des conventions toujours en cours de validité
     */
    List<Convention> findByDateExpirationAfter(LocalDate date);

    List<Convention> findByArchiveeFalseOrderByDateExpirationAsc();

    List<Convention> findByArchiveeTrueOrderByDateExpirationDesc();

    boolean existsByTitreIgnoreCaseAndPartenaireId(String titre, Long partenaireId);
}
