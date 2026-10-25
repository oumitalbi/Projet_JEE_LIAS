package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.Materiel;
import com.lias.gestion_laboratoire.models.enums.EtatMateriel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;

/**
 * Repository Spring Data JPA pour l'entité {@link Materiel}.
 *
 * <p>Fournit les opérations CRUD de base et des méthodes de recherche
 * spécifiques au module Logistique pour gérer l'inventaire du laboratoire.</p>
 */
@Repository
public interface MaterielRepository extends JpaRepository<Materiel, Long> {

    /**
     * Recherche les matériels en fonction de leur état physique.
     * 
     * @param etat l'état recherché (NEUF, BON_ETAT, EN_PANNE...)
     * @return liste des matériels correspondants
     */
    List<Materiel> findByEtat(EtatMateriel etat);

    /**
     * Recherche tous les équipements actuellement affectés à un membre spécifique.
     * 
     * @param membreId l'identifiant du membre
     * @return liste des matériels en possession du membre
     */
    List<Materiel> findByMembreAffecteId(Long membreId);

    /**
     * Recherche tous les matériels disponibles en stock (non affectés).
     * 
     * <p>Sert principalement au responsable logistique pour voir quels
     * équipements peuvent être assignés à une nouvelle demande.</p>
     * 
     * @return liste des matériels dont le champ {@code membreAffecte} est null
     */
    List<Materiel> findByMembreAffecteIsNull();

    List<Materiel> findAllByOrderByCategorieAscNomAsc();

    boolean existsByNumeroInventaireIgnoreCase(String numeroInventaire);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Materiel m where m.id = :id")
    java.util.Optional<Materiel> findByIdForUpdate(@Param("id") Long id);
}
