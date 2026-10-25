package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.DemandeAdhesion;
import com.lias.gestion_laboratoire.models.enums.StatutAdhesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface DemandeAdhesionRepository extends JpaRepository<DemandeAdhesion, Long> {
    List<DemandeAdhesion> findByStatutOrderByDateDemandeDesc(StatutAdhesion statut);

    List<DemandeAdhesion> findAllByOrderByDateDemandeDesc();

    List<DemandeAdhesion> findByDateDemandeBetween(LocalDateTime debut, LocalDateTime fin);

    List<DemandeAdhesion> findByDateTraitementBetween(LocalDateTime debut, LocalDateTime fin);

    List<DemandeAdhesion> findByStatutNotOrderByDateDemandeDesc(StatutAdhesion statut);

    boolean existsByEmailIgnoreCaseAndStatut(String email, StatutAdhesion statut);
}
