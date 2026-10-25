package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.DemandeMateriel;
import com.lias.gestion_laboratoire.models.enums.StatutDemande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.time.LocalDateTime;

/**
 * Repository Spring Data JPA pour l'entité {@link DemandeMateriel}.
 *
 * <p>Fournit les opérations CRUD de base et des méthodes de recherche
 * spécifiques pour le suivi du workflow des demandes logistiques.</p>
 */
@Repository
public interface DemandeMaterielRepository extends JpaRepository<DemandeMateriel, Long> {

    /**
     * Récupère l'historique complet des demandes formulées par un membre.
     * 
     * @param membreId l'identifiant du membre demandeur
     * @return liste des demandes effectuées par ce membre
     */
    List<DemandeMateriel> findByDemandeurId(Long membreId);

    /**
     * Récupère les demandes filtrées par leur statut.
     * 
     * <p>Particulièrement utile pour les administrateurs afin de lister
     * toutes les demandes en attente de validation.</p>
     * 
     * @param statut le statut recherché (EN_ATTENTE, VALIDEE...)
     * @return liste des demandes correspondantes
     */
    List<DemandeMateriel> findByStatut(StatutDemande statut);

    List<DemandeMateriel> findAllByOrderByDateDemandeDesc();

    List<DemandeMateriel> findByDateDemandeBetween(LocalDateTime debut, LocalDateTime fin);

    List<DemandeMateriel> findByDemandeurIdOrderByDateDemandeDesc(Long membreId);

    boolean existsByDemandeurIdAndNomBesoin(Long demandeurId, String nomBesoin);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DemandeMateriel d where d.id = :id")
    java.util.Optional<DemandeMateriel> findByIdForUpdate(@Param("id") Long id);
}
