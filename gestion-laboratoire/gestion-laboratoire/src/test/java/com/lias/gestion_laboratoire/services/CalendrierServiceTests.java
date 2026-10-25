package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.authorization.AuthorizationService;
import com.lias.gestion_laboratoire.models.Evenement;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.Reunion;
import com.lias.gestion_laboratoire.models.enums.TypeEvenement;
import com.lias.gestion_laboratoire.repositories.EvenementRepository;
import com.lias.gestion_laboratoire.repositories.ReunionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendrierServiceTests {

    @Mock private EvenementRepository evenementRepository;
    @Mock private ReunionRepository reunionRepository;
    @Mock private AuthorizationService authorizationService;
    @InjectMocks private CalendrierService calendrierService;

    @Test
    void returnsEventsMeetingsAndCancelledEventsForTheRequestedMonth() {
        Membre membre = new Membre();
        membre.setId(8L);
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 31, 23, 59, 59);

        Evenement evenement = new Evenement();
        evenement.setId(1L);
        evenement.setTitre("Conférence");
        evenement.setType(TypeEvenement.CONFERENCE);
        evenement.setDateDebut(LocalDateTime.of(2026, 10, 12, 9, 0));
        evenement.setDateFin(LocalDateTime.of(2026, 10, 12, 17, 0));

        Evenement annule = new Evenement();
        annule.setId(2L);
        annule.setTitre("Workshop annulé");
        annule.setType(TypeEvenement.WORKSHOP);
        annule.setDateDebut(LocalDateTime.of(2026, 10, 20, 9, 0));
        annule.setDateFin(LocalDateTime.of(2026, 10, 20, 12, 0));
        annule.setEstAnnule(true);

        Reunion reunion = new Reunion();
        reunion.setId(3L);
        reunion.setTitre("Réunion LIAS");
        reunion.setDateReunion(LocalDateTime.of(2026, 10, 25, 10, 0));
        reunion.setStatut(com.lias.gestion_laboratoire.models.enums.StatutReunion.PLANIFIEE);

        when(evenementRepository.findActiveIntersectingPeriod(start, end))
                .thenReturn(List.of(evenement));
        when(authorizationService.estSupervisionPourEvenements(membre)).thenReturn(true);
        when(evenementRepository.findCancelledIntersectingPeriod(start, end))
                .thenReturn(List.of(annule));
        when(reunionRepository.findCalendarItemsIntersectingPeriod(start, end)).thenReturn(List.of(reunion));
        when(authorizationService.peutConsulterReunion(membre, reunion)).thenReturn(true);

        var result = calendrierService.obtenirEvenementsParMois(2026, 10, membre);

        assertEquals(3, result.size());
        assertEquals("EVENEMENT", result.get(0).source());
        assertTrue(result.get(1).annule());
        assertEquals("REUNION", result.get(2).source());
    }
}
