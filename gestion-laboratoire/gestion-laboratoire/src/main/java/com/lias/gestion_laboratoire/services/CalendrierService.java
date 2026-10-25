package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.authorization.AuthorizationService;
import com.lias.gestion_laboratoire.dto.CalendrierItemResponse;
import com.lias.gestion_laboratoire.models.Evenement;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.Reunion;
import com.lias.gestion_laboratoire.repositories.ReunionRepository;
import com.lias.gestion_laboratoire.repositories.EvenementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CalendrierService {

    private final EvenementRepository evenementRepository;
    private final ReunionRepository reunionRepository;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public List<CalendrierItemResponse> obtenirEvenementsParMois(int annee, int mois, Membre membre) {
        authorizationService.verifierLectureEvenements(membre);
        authorizationService.verifierAccesReunions(membre);
        YearMonth yearMonth = YearMonth.of(annee, mois);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        List<CalendrierItemResponse> items = new ArrayList<>();

        evenementRepository.findActiveIntersectingPeriod(start, end)
                .stream()
                .map(evenement -> evenementItem(evenement, membre))
                .forEach(items::add);

        boolean supervisionEvenements = authorizationService.estSupervisionPourEvenements(membre);
        var evenementsAnnules = supervisionEvenements
                ? evenementRepository.findCancelledIntersectingPeriod(start, end)
                : evenementRepository.findByOrganisateurs_IdAndEstAnnuleTrueOrderByDateDebutDesc(membre.getId())
                        .stream()
                        .filter(evenement -> !evenement.getDateDebut().isAfter(end)
                                && evenement.getDateFin() != null && !evenement.getDateFin().isBefore(start))
                        .toList();
        evenementsAnnules.stream()
                .map(evenement -> evenementItem(evenement, membre))
                .forEach(items::add);

        reunionRepository.findCalendarItemsIntersectingPeriod(start, end).stream()
                .filter(reunion -> authorizationService.peutConsulterReunion(membre, reunion))
                .map(reunion -> reunionItem(reunion, membre))
                .forEach(items::add);

        return items.stream()
                .sorted(java.util.Comparator.comparing(CalendrierItemResponse::dateDebut,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();
    }

    private CalendrierItemResponse evenementItem(Evenement evenement, Membre membre) {
        boolean concerne = evenement.getOrganisateurs().stream()
                .anyMatch(personne -> Objects.equals(personne.getId(), membre.getId()))
                || evenement.getParticipants().stream()
                .anyMatch(personne -> Objects.equals(personne.getId(), membre.getId()));
        return new CalendrierItemResponse(
                evenement.getId(),
                "EVENEMENT",
                evenement.getTitre(),
                evenement.getType() == null ? "EVENEMENT" : evenement.getType().name(),
                evenement.getDateDebut(),
                evenement.getDateFin(),
                evenement.getLieu(),
                evenement.isEstAnnule() ? "ANNULE" : statutEvenement(evenement.getDateDebut(), evenement.getDateFin()),
                evenement.isEstAnnule(),
                concerne,
                evenement.getDescription(),
                evenement.getOrganisateurs().stream().map(Membre::getId).toList(),
                evenement.getParticipants().stream().map(Membre::getId).toList()
        );
    }

    private CalendrierItemResponse reunionItem(Reunion reunion, Membre membre) {
        boolean concerne = Objects.equals(reunion.getOrganisateur() == null ? null : reunion.getOrganisateur().getId(),
                membre.getId())
                || reunion.getParticipants().stream()
                .anyMatch(personne -> Objects.equals(personne.getId(), membre.getId()));
        return new CalendrierItemResponse(
                reunion.getId(),
                "REUNION",
                reunion.getTitre(),
                "REUNION",
                reunion.getDateReunion(),
                reunion.getDateFin(),
                reunion.getLieu(),
                reunion.getStatut() == null ? "PLANIFIEE" : reunion.getStatut().name(),
                reunion.getStatut() != null && reunion.getStatut().name().equals("ANNULEE"),
                concerne,
                reunion.getOrdreDuJour(),
                reunion.getOrganisateur() == null ? List.of() : List.of(reunion.getOrganisateur().getId()),
                reunion.getParticipants().stream().map(Membre::getId).toList()
        );
    }

    private String statutEvenement(LocalDateTime debut, LocalDateTime fin) {
        LocalDateTime maintenant = LocalDateTime.now();
        if (fin != null && fin.isBefore(maintenant)) return "PASSE";
        if (debut != null && debut.isAfter(maintenant)) return "A_VENIR";
        return "EN_COURS";
    }
}
