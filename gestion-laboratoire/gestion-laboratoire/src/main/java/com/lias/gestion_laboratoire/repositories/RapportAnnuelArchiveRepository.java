package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.RapportAnnuelArchive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RapportAnnuelArchiveRepository extends JpaRepository<RapportAnnuelArchive, Long> {
    List<RapportAnnuelArchive> findAllByOrderByDateGenerationDesc();
    List<RapportAnnuelArchive> findByAnneeOrderByVersionDesc(int annee);
}
