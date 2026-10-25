package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.ReunionProcesVerbal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReunionProcesVerbalRepository extends JpaRepository<ReunionProcesVerbal, Long> {
    List<ReunionProcesVerbal> findByReunionIdOrderByDateAjoutDesc(Long reunionId);
}
