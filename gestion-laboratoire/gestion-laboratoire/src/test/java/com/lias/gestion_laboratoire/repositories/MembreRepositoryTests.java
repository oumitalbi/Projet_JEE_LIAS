package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.Statut;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MembreRepositoryTests {

    @Autowired
    private MembreRepository membreRepository;

    @Test
    void testRechercheMembresExclutComptesInactifs() {
        // Given - 1 membre actif qui correspond au mot-clé
        Membre activeMember = new Membre();
        activeMember.setNom("Dupont");
        activeMember.setPrenom("Jean");
        activeMember.setEmail("jean.dupont@lias.fr");
        activeMember.setStatut(Statut.PERMANENT);
        activeMember.setRole(Role.MEMBRE_EQUIPE);
        activeMember.setEquipeLaboratoire(EquipeLaboratoire.ILIAS);
        activeMember.setCompteActif(true);
        membreRepository.save(activeMember);

        // Given - 1 membre inactif qui correspond aussi au mot-clé
        Membre inactiveMember = new Membre();
        inactiveMember.setNom("Dupond");
        inactiveMember.setPrenom("Marie");
        inactiveMember.setEmail("marie.dupond@lias.fr");
        inactiveMember.setStatut(Statut.ANCIEN);
        inactiveMember.setRole(Role.MEMBRE_EQUIPE);
        inactiveMember.setEquipeLaboratoire(EquipeLaboratoire.AUCUNE);
        inactiveMember.setCompteActif(false);
        membreRepository.save(inactiveMember);

        // When
        List<Membre> resultats = membreRepository
                .findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCaseAndCompteActif(
                        "Dupon", "Dupon", true);

        // Then - Seul le membre actif doit être retourné
        assertThat(resultats)
                .hasSize(1)
                .extracting(Membre::getNom)
                .containsExactly("Dupont");
    }
}
