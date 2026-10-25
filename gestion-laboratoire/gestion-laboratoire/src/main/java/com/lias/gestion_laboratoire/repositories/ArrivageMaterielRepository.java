package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.ArrivageMateriel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArrivageMaterielRepository extends JpaRepository<ArrivageMateriel, Long> {
    List<ArrivageMateriel> findAllByOrderByDateArrivageDesc();

    List<ArrivageMateriel> findByMaterielIdOrderByDateArrivageDesc(Long materielId);

    boolean existsByMaterielIdAndBonLivraisonIgnoreCase(Long materielId, String bonLivraison);
}
