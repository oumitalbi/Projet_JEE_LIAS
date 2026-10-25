package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.authorization.AuthorizationService;
import com.lias.gestion_laboratoire.models.Evenement;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.models.enums.TypeNotification;
import com.lias.gestion_laboratoire.repositories.EvenementRepository;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvenementServiceTests {

    @Mock
    private EvenementRepository evenementRepository;
    @Mock
    private MembreRepository membreRepository;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private EvenementService evenementService;

    @Test
    void refusesRemovalOfLastOrganizer() {
        Membre organizer = membre(1L);
        Evenement evenement = evenement();
        evenement.setOrganisateurs(new LinkedHashSet<>());
        evenement.getOrganisateurs().add(organizer);
        when(evenementRepository.findWithParticipantsAndOrganisateursById(5L)).thenReturn(Optional.of(evenement));
        when(membreRepository.findById(1L)).thenReturn(Optional.of(organizer));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> evenementService.retirerOrganisateur(5L, 1L, organizer));

        assertEquals(400, exception.getStatusCode().value());
        verify(evenementRepository, never()).save(any(Evenement.class));
    }

    @Test
    void repeatedRegistrationDoesNotDuplicateParticipant() {
        Membre participant = membre(2L);
        Evenement evenement = evenement();
        evenement.setDateDebut(LocalDateTime.now().plusDays(2));
        evenement.setDateFin(LocalDateTime.now().plusDays(2).plusHours(1));
        when(evenementRepository.findWithParticipantsAndOrganisateursById(5L)).thenReturn(Optional.of(evenement));
        when(evenementRepository.save(any(Evenement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        evenementService.inscrire(5L, participant);
        var response = evenementService.inscrire(5L, participant);

        assertEquals(1, evenement.getParticipants().size());
        assertEquals(true, response.estInscrit());
        verify(evenementRepository).save(evenement);
    }

    @Test
    void repeatedCancellationDoesNotSaveOrNotifyAgain() {
        Membre organizer = membre(1L);
        Evenement evenement = evenement();
        evenement.getOrganisateurs().add(organizer);
        when(evenementRepository.findWithParticipantsAndOrganisateursById(5L)).thenReturn(Optional.of(evenement));
        when(evenementRepository.save(any(Evenement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        evenementService.annulerEvenement(5L, "Motif", organizer);
        evenementService.annulerEvenement(5L, "Motif différent", organizer);

        verify(evenementRepository, times(1)).save(evenement);
        verify(notificationService, times(1)).creerNotification(
                anyLong(), anyString(), anyString(), eq(TypeNotification.EVENEMENT), eq("EVENEMENT"), anyLong());
    }

    private Evenement evenement() {
        Evenement evenement = new Evenement();
        evenement.setId(5L);
        evenement.setTitre("Événement de test");
        evenement.setDateDebut(LocalDateTime.now().plusDays(1));
        evenement.setDateFin(LocalDateTime.now().plusDays(1).plusHours(1));
        evenement.setOrganisateurs(new LinkedHashSet<>());
        evenement.setParticipants(new LinkedHashSet<>());
        return evenement;
    }

    private Membre membre(Long id) {
        Membre membre = new Membre();
        membre.setId(id);
        membre.setNom("Test");
        membre.setPrenom("Membre");
        membre.setStatut(Statut.PERMANENT);
        membre.setCompteActif(true);
        membre.setCompteGele(false);
        return membre;
    }
}
