package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.HistoriqueRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoriqueRoleRepository extends JpaRepository<HistoriqueRole, Long> {
    List<HistoriqueRole> findByMembreIdOrderByDateChangementDesc(Long membreId);
}
