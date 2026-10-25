package com.lias.gestion_laboratoire.authorization;

import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.RoleUtilisateur;
import com.lias.gestion_laboratoire.models.enums.Statut;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationServiceConventionTests {

    private final AuthorizationService authorizationService = new AuthorizationService();

    @Test
    void allowsActivePermanentAndAssociateMembersToRead() {
        assertDoesNotThrow(() -> authorizationService.verifierAccesConventions(
                member(Statut.PERMANENT, Role.MEMBRE_EQUIPE, null)));
        assertDoesNotThrow(() -> authorizationService.verifierAccesConventions(
                member(Statut.ASSOCIE, Role.MEMBRE_EQUIPE, null)));
    }

    @Test
    void deniesDoctorantsAndFrozenAccounts() {
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> authorizationService.verifierAccesConventions(
                        member(Statut.DOCTORANT, Role.MEMBRE_EQUIPE, null))).getStatusCode().value());

        Membre frozen = member(Statut.PERMANENT, Role.MEMBRE_EQUIPE, null);
        frozen.setCompteGele(true);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> authorizationService.verifierAccesConventions(frozen)).getStatusCode().value());
    }

    @Test
    void allowsOnlyDirectorToCreateConvention() {
        assertDoesNotThrow(() -> authorizationService.verifierCreationConvention(
                member(Statut.PERMANENT, Role.DIRECTEUR, null)));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> authorizationService.verifierCreationConvention(
                        member(Statut.PERMANENT, Role.VICE_DIRECTEUR, null))).getStatusCode().value());
    }

    @Test
    void allowsSupervisorToManageConvention() {
        assertDoesNotThrow(() -> authorizationService.verifierGestionPartenaire(
                member(Statut.PERMANENT, Role.ADMIN, RoleUtilisateur.ADMIN)));
    }

    private Membre member(Statut status, Role role, RoleUtilisateur systemRole) {
        Membre member = new Membre();
        member.setStatut(status);
        member.setRole(role);
        member.setRoleSysteme(systemRole);
        member.setCompteActif(true);
        member.setCompteGele(false);
        return member;
    }
}
