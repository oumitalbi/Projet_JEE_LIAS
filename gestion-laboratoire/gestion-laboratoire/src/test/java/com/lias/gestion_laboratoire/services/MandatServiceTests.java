package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.dto.MandatClotureRequest;
import com.lias.gestion_laboratoire.dto.MandatRequest;
import com.lias.gestion_laboratoire.models.Mandat;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.RoleUtilisateur;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.repositories.HistoriqueRoleRepository;
import com.lias.gestion_laboratoire.repositories.MandatRepository;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MandatServiceTests {
    private MandatRepository mandatRepository;
    private MembreRepository membreRepository;
    private MandatService service;
    private Membre admin;

    @BeforeEach
    void setUp() {
        mandatRepository = mock(MandatRepository.class);
        membreRepository = mock(MembreRepository.class);
        service = new MandatService(mandatRepository, membreRepository,
                mock(HistoriqueRoleRepository.class), mock(NotificationService.class));
        admin = new Membre();
        admin.setId(1L);
        admin.setRoleSysteme(RoleUtilisateur.ADMIN);
        admin.setStatut(Statut.PERMANENT);
        admin.setCompteActif(true);
        when(membreRepository.findById(1L)).thenReturn(Optional.of(admin));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void refusesDoctorantAsMandateHolder() {
        Membre doctorant = new Membre();
        doctorant.setId(2L);
        doctorant.setStatut(Statut.DOCTORANT);
        doctorant.setCompteActif(true);
        when(membreRepository.findById(2L)).thenReturn(Optional.of(doctorant));

        MandatRequest request = new MandatRequest();
        request.setMembreId(2L);
        request.setResponsabilite(Role.DIRECTEUR);
        request.setDateDebut(LocalDate.now());

        assertThrows(AccessDeniedException.class, () -> service.assignerNouveauMandat(request));
        verify(mandatRepository, never()).save(any(Mandat.class));
    }

    @Test
    void refusesToCloseLastDirectorWithoutSuccessor() {
        Mandat mandate = new Mandat();
        mandate.setId(10L);
        mandate.setTitulaire(admin);
        mandate.setResponsabilite(Role.DIRECTEUR);
        mandate.setDateDebut(LocalDate.now().minusYears(1));
        when(mandatRepository.findById(10L)).thenReturn(Optional.of(mandate));
        when(mandatRepository.findMandatsRoleValidesAu(eq(Role.DIRECTEUR), any(LocalDate.class)))
                .thenReturn(List.of(mandate));

        MandatClotureRequest request = new MandatClotureRequest();
        request.setDateFin(LocalDate.now());
        request.setMotif("Fin de mandat");

        assertThrows(IllegalArgumentException.class, () -> service.cloturerMandat(10L, request));
        verify(mandatRepository, never()).save(any(Mandat.class));
    }
}
