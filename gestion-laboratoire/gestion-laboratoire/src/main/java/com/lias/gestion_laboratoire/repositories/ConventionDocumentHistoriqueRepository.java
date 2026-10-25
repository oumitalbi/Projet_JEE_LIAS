package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.ConventionDocumentHistorique;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConventionDocumentHistoriqueRepository extends JpaRepository<ConventionDocumentHistorique, Long> {
    List<ConventionDocumentHistorique> findByConventionIdOrderByDateAjoutDesc(Long conventionId);

    boolean existsByConventionIdAndDocumentId(Long conventionId, Long documentId);
}
