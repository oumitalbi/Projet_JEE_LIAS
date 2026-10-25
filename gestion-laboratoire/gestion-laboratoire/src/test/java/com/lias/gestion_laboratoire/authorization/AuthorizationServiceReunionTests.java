package com.lias.gestion_laboratoire.authorization;

import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.Reunion;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.Statut;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationServiceReunionTests {

    private final AuthorizationService authorizationService = new AuthorizationService();

    @Test
    void deniesDoctorantAccessToMeetings() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authorizationService.verifierAccesReunions(member(1L, Statut.DOCTORANT, Role.MEMBRE_EQUIPE)));
        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void deniesNonParticipantAccessToMeeting() {
        Membre outsider = member(2L, Statut.PERMANENT, Role.MEMBRE_EQUIPE);
        Reunion reunion = new Reunion();
        reunion.setOrganisateur(member(1L, Statut.PERMANENT, Role.CHEF_EQUIPE));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authorizationService.verifierConsultationReunion(outsider, reunion));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void deniesNonOrganizerToManageMeeting() {
        Membre outsider = member(2L, Statut.PERMANENT, Role.MEMBRE_EQUIPE);
        Reunion reunion = new Reunion();
        reunion.setOrganisateur(member(1L, Statut.PERMANENT, Role.CHEF_EQUIPE));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authorizationService.verifierGestionReunion(outsider, reunion));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void allowsParticipantToReadMeeting() {
        Membre participant = member(2L, Statut.PERMANENT, Role.MEMBRE_EQUIPE);
        Reunion reunion = new Reunion();
        reunion.getParticipants().add(participant);

        assertDoesNotThrow(() -> authorizationService.verifierConsultationReunion(participant, reunion));
    }

    @Test
    void deniesPlanningByDoctorant() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authorizationService.verifierPlanificationReunion(
                        member(1L, Statut.DOCTORANT, Role.CHEF_EQUIPE)));
        assertEquals(403, exception.getStatusCode().value());
    }

    private Membre member(Long id, Statut statut, Role role) {
        Membre membre = new Membre();
        membre.setId(id);
        membre.setStatut(statut);
        membre.setRole(role);
        membre.setCompteActif(true);
        membre.setCompteGele(false);
        return membre;
    }
}
