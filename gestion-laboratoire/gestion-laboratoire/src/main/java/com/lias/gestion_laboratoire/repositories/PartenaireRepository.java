package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.Partenaire;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository Spring Data JPA pour l'entité {@link Partenaire}.
 *
 * <p>Fournit les opérations CRUD standard pour gérer le répertoire
 * des organismes partenaires (universités, entreprises, etc.) du laboratoire.</p>
 */
@Repository
public interface PartenaireRepository extends JpaRepository<Partenaire, Long> {
    @Query("select p from Partenaire p where p.archive = false or p.archive is null order by p.nom asc")
    List<Partenaire> findByArchiveFalseOrderByNomAsc();

    List<Partenaire> findByArchiveTrueOrderByNomAsc();

    boolean existsByNomIgnoreCase(String nom);

    boolean existsByNomIgnoreCaseAndIdNot(String nom, Long id);
}
