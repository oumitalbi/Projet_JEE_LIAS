package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.authorization.AuthorizationService;
import com.lias.gestion_laboratoire.models.DemandeMateriel;
import com.lias.gestion_laboratoire.models.Materiel;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.EtatMateriel;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.models.enums.StatutDemande;
import com.lias.gestion_laboratoire.repositories.ArrivageMaterielRepository;
import com.lias.gestion_laboratoire.repositories.DemandeMaterielRepository;
import com.lias.gestion_laboratoire.repositories.MaterielRepository;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LogistiqueServiceWorkflowTests {
    private final MaterielRepository materialRepository = mock(MaterielRepository.class);
    private final DemandeMaterielRepository requestRepository = mock(DemandeMaterielRepository.class);
    private final ArrivageMaterielRepository arrivalRepository = mock(ArrivageMaterielRepository.class);
    private final MembreRepository memberRepository = mock(MembreRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final LogistiqueService service = new LogistiqueService(materialRepository, requestRepository,
            arrivalRepository, memberRepository, new AuthorizationService(), notificationService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approvalDecrementsStockOnlyOnce() {
        Membre director = member(1L, Role.DIRECTEUR);
        Membre requester = member(2L, Role.MEMBRE_EQUIPE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(director, null, List.of()));
        Materiel material = new Materiel();
        material.setId(4L);
        material.setNom("Serveur GPU");
        material.setCategorie("Informatique");
        material.setEtat(EtatMateriel.BON_ETAT);
        material.setQuantiteTotale(5);
        material.setQuantiteDisponible(5);
        DemandeMateriel request = new DemandeMateriel();
        request.setId(7L);
        request.setDemandeur(requester);
        request.setMaterielDemande(material);
        request.setQuantiteDemandee(2);
        request.setStatut(StatutDemande.EN_ATTENTE);
        when(requestRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(request));
        when(materialRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(material));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(requester));
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(materialRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.validerDemande(7L, null);
        assertEquals(3, material.getQuantiteDisponible());
        assertEquals(StatutDemande.VALIDEE, request.getStatut());
        service.validerDemande(7L, null);
        assertEquals(3, material.getQuantiteDisponible());
        verify(materialRepository, times(1)).findByIdForUpdate(4L);
    }

    @Test
    void refusesApprovalWhenThereIsNoStock() {
        Membre director = member(1L, Role.DIRECTEUR);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(director, null, List.of()));
        Materiel material = new Materiel();
        material.setId(4L);
        material.setEtat(EtatMateriel.BON_ETAT);
        material.setQuantiteTotale(1);
        material.setQuantiteDisponible(0);
        DemandeMateriel request = new DemandeMateriel();
        request.setId(7L);
        request.setDemandeur(member(2L, Role.MEMBRE_EQUIPE));
        request.setMaterielDemande(material);
        request.setQuantiteDemandee(1);
        request.setStatut(StatutDemande.EN_ATTENTE);
        when(requestRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(request));
        when(materialRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(material));

        assertThrows(ResponseStatusException.class, () -> service.validerDemande(7L, null));
        assertEquals(0, material.getQuantiteDisponible());
        assertEquals(StatutDemande.EN_ATTENTE, request.getStatut());
        verify(materialRepository, never()).save(any());
    }

    @Test
    void returnReplenishesLoanStockOnlyOnce() {
        Membre requester = member(2L, Role.MEMBRE_EQUIPE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(requester, null, List.of()));
        Materiel material = new Materiel();
        material.setId(4L);
        material.setNom("Ordinateur");
        material.setCategorie("Informatique");
        material.setQuantiteTotale(5);
        material.setQuantiteDisponible(3);
        DemandeMateriel request = new DemandeMateriel();
        request.setId(7L);
        request.setDemandeur(requester);
        request.setMaterielAssigne(material);
        request.setQuantiteDemandee(2);
        request.setStatut(StatutDemande.VALIDEE);
        when(requestRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(request));
        when(materialRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(material));
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(materialRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.retournerMateriel(7L);
        assertEquals(5, material.getQuantiteDisponible());
        assertEquals(StatutDemande.RETOURNEE, request.getStatut());
        service.retournerMateriel(7L);
        assertEquals(5, material.getQuantiteDisponible());
        verify(materialRepository, times(1)).findByIdForUpdate(4L);
    }

    private Membre member(Long id, Role role) {
        Membre member = new Membre();
        member.setId(id);
        member.setNom("Membre");
        member.setPrenom("Test");
        member.setStatut(Statut.PERMANENT);
        member.setRole(role);
        member.setCompteActif(true);
        member.setCompteGele(false);
        return member;
    }
}
