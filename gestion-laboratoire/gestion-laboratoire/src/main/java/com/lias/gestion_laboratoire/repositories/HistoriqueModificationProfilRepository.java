package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.HistoriqueModificationProfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoriqueModificationProfilRepository extends JpaRepository<HistoriqueModificationProfil, Long> {
    List<HistoriqueModificationProfil> findByMembreIdOrderByDateActionDesc(Long membreId);
}
