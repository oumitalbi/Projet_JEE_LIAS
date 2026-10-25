package com.lias.gestion_laboratoire.authorization;

import com.lias.gestion_laboratoire.models.Evenement;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.Statut;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationServiceEvenementTests {

    private final AuthorizationService authorizationService = new AuthorizationService();

    @Test
    void deniesModificationByNonOrganizer() {
        Membre organisateur = membre(10L, Statut.PERMANENT);
        Membre autreMembre = membre(20L, Statut.PERMANENT);
        Evenement evenement = new Evenement();
        evenement.getOrganisateurs().add(organisateur);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authorizationService.verifierGestionEvenement(evenement, autreMembre));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void deniesEventCreationByDoctorant() {
        Membre doctorant = membre(20L, Statut.DOCTORANT);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authorizationService.verifierCreationEvenement(doctorant));

        assertEquals(403, exception.getStatusCode().value());
    }

    private Membre membre(Long id, Statut statut) {
        Membre membre = new Membre();
        membre.setId(id);
        membre.setStatut(statut);
        membre.setRole(Role.MEMBRE_EQUIPE);
        membre.setCompteActif(true);
        membre.setCompteGele(false);
        return membre;
    }
}
