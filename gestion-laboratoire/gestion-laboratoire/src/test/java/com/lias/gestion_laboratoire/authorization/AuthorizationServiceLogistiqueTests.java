package com.lias.gestion_laboratoire.authorization;

import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.RoleUtilisateur;
import com.lias.gestion_laboratoire.models.enums.Statut;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationServiceLogistiqueTests {
    private final AuthorizationService authorizationService = new AuthorizationService();

    @Test
    void permanentAndAssociateMembersCanAccessAndSubmitRequests() {
        assertDoesNotThrow(() -> authorizationService.verifierSoumissionDemandeLogistique(
                member(Statut.PERMANENT, Role.MEMBRE_EQUIPE, null)));
        assertDoesNotThrow(() -> authorizationService.verifierSoumissionDemandeLogistique(
                member(Statut.ASSOCIE, Role.MEMBRE_EQUIPE, RoleUtilisateur.MEMBRE_ASSOCIE)));
    }

    @Test
    void refusesDoctorantsRetiredAndFrozenMembers() {
        for (Statut status : new Statut[]{Statut.DOCTORANT, Statut.RETRAITE, Statut.ANCIEN}) {
            assertEquals(403, assertThrows(ResponseStatusException.class,
                    () -> authorizationService.verifierAccesLogistique(
                            member(status, Role.MEMBRE_EQUIPE, null))).getStatusCode().value());
        }
        Membre frozen = member(Statut.PERMANENT, Role.MEMBRE_EQUIPE, null);
        frozen.setCompteGele(true);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> authorizationService.verifierAccesLogistique(frozen)).getStatusCode().value());

        Membre inconsistentDoctorant = member(Statut.PERMANENT, Role.MEMBRE_EQUIPE, RoleUtilisateur.DOCTORANT);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> authorizationService.verifierAccesLogistique(inconsistentDoctorant)).getStatusCode().value());
    }

    @Test
    void onlyDirectorCanManageInventoryAndApproveRequests() {
        assertDoesNotThrow(() -> authorizationService.verifierGestionLogistique(
                member(Statut.PERMANENT, Role.DIRECTEUR, RoleUtilisateur.DIRECTEUR)));
        for (Role role : new Role[]{Role.ADMIN, Role.VICE_DIRECTEUR, Role.CHEF_EQUIPE}) {
            assertEquals(403, assertThrows(ResponseStatusException.class,
                    () -> authorizationService.verifierGestionLogistique(
                            member(Statut.PERMANENT, role, null))).getStatusCode().value());
        }
    }

    @Test
    void formerDirectorSystemRoleDoesNotPreserveFunctionalAccess() {
        Membre ancienDirecteur = member(Statut.PERMANENT, Role.MEMBRE_EQUIPE, RoleUtilisateur.DIRECTEUR);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> authorizationService.verifierGestionLogistique(ancienDirecteur))
                .getStatusCode().value());
    }

    private Membre member(Statut status, Role role, RoleUtilisateur systemRole) {
        Membre member = new Membre();
        member.setId(42L);
        member.setStatut(status);
        member.setRole(role);
        member.setRoleSysteme(systemRole);
        member.setCompteActif(true);
        member.setCompteGele(false);
        return member;
    }
}
