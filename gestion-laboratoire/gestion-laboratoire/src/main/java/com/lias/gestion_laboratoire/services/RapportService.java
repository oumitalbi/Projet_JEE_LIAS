package com.lias.gestion_laboratoire.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lias.gestion_laboratoire.authorization.AuthorizationService;
import com.lias.gestion_laboratoire.dto.RapportAnnuelDTO;
import com.lias.gestion_laboratoire.models.*;
import com.lias.gestion_laboratoire.models.enums.*;
import com.lias.gestion_laboratoire.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RapportService {
    private final EvenementRepository evenementRepository;
    private final PublicationRepository publicationRepository;
    private final MembreRepository membreRepository;
    private final ReunionRepository reunionRepository;
    private final ConventionRepository conventionRepository;
    private final DemandeMaterielRepository demandeMaterielRepository;
    private final MandatRepository mandatRepository;
    private final DemandeAdhesionRepository demandeAdhesionRepository;
    private final HistoriqueAffiliationRepository historiqueAffiliationRepository;
    private final RapportAnnuelArchiveRepository archiveRepository;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public RapportAnnuelDTO genererRapportAnnuel(int annee, Membre membre) {
        verifierAcces(membre);
        int anneeActuelle = LocalDate.now().getYear();
        if (annee < 2000 || annee > anneeActuelle) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'année doit être comprise entre 2000 et " + anneeActuelle + ".");
        }

        LocalDate debut = LocalDate.of(annee, 1, 1);
        LocalDate fin = LocalDate.of(annee, 12, 31);
        LocalDateTime debutDateTime = debut.atStartOfDay();
        LocalDateTime finDateTime = fin.atTime(LocalTime.MAX);

        List<Evenement> evenements = evenementRepository
                .findByDateDebutBetweenAndEstAnnuleFalseOrderByDateDebutAsc(debutDateTime, finDateTime);
        List<Publication> publications = publicationRepository
                .findByDatePublicationBetweenAndEstArchive(debut, fin, false);
        List<Membre> nouveauxMembres = membreRepository.findByDateAffiliationBetween(debut, fin);
        List<Reunion> reunions = reunionRepository.findByDateReunionBetween(debutDateTime, finDateTime)
                .stream().filter(reunion -> !reunion.isArchivee()).toList();
        List<Convention> conventionsActives = conventionRepository.findByArchiveeFalseOrderByDateExpirationAsc()
                .stream()
                .filter(convention -> convention.getDateSignature() == null
                        || !convention.getDateSignature().isAfter(fin))
                .filter(convention -> convention.getDateExpiration() == null
                        || !convention.getDateExpiration().isBefore(debut))
                .toList();
        List<DemandeMateriel> demandesLogistiques =
                demandeMaterielRepository.findByDateDemandeBetween(debutDateTime, finDateTime);
        List<Mandat> mandats = mandatRepository.findIntersectingPeriod(debut, fin);
        List<DemandeAdhesion> demandesAdhesion =
                demandeAdhesionRepository.findByDateDemandeBetween(debutDateTime, finDateTime);
        List<DemandeAdhesion> adhesionsTraitees =
                demandeAdhesionRepository.findByDateTraitementBetween(debutDateTime, finDateTime);
        List<HistoriqueAffiliation> mouvements =
                historiqueAffiliationRepository.findChangedWithinPeriod(debut, fin);

        RapportAnnuelDTO rapport = RapportAnnuelDTO.builder()
                .annee(annee)
                .nbEvenements(evenements.size())
                .nbPublications(publications.size())
                .nbReunions(reunions.size())
                .nbConventionsActives(conventionsActives.size())
                .nbDemandesLogistiques(demandesLogistiques.size())
                .nbMandats(mandats.size())
                .nbNouveauxMembres(nouveauxMembres.size())
                .nbDemandesAdhesion(demandesAdhesion.size())
                .evenementsParType(comptesEnum(TypeEvenement.values(), evenements.stream()
                        .map(Evenement::getType).toList()))
                .publicationsParType(comptesEnum(TypePublication.values(), publications.stream()
                        .map(Publication::getType).toList()))
                .publicationsParEquipe(comptesEquipes(publications))
                .logistiqueParStatut(comptesEnum(StatutDemande.values(), demandesLogistiques.stream()
                        .map(DemandeMateriel::getStatut).toList()))
                .mandatsParRole(comptesEnum(Role.values(), mandats.stream()
                        .map(Mandat::getResponsabilite).toList()))
                .membresParStatut(comptesEnum(Statut.values(), mouvements.stream()
                        .map(HistoriqueAffiliation::getStatut).toList()))
                .adhesionsParDecision(comptesEnum(StatutAdhesion.values(), adhesionsTraitees.stream()
                        .map(DemandeAdhesion::getStatut).toList()))
                .build();

        int version = archiveRepository.findByAnneeOrderByVersionDesc(annee).stream()
                .findFirst().map(RapportAnnuelArchive::getVersion).orElse(0) + 1;
        RapportAnnuelArchive archive = new RapportAnnuelArchive();
        archive.setAnnee(annee);
        archive.setVersion(version);
        archive.setDateGeneration(LocalDateTime.now());
        archive.setGenerePar(membre);
        rapport.setVersion(version);
        rapport.setDateGeneration(archive.getDateGeneration());
        rapport.setGenerePar(membre.getPrenom() + " " + membre.getNom());
        archive.setDonneesJson(serialiser(rapport));
        RapportAnnuelArchive sauvegarde = archiveRepository.save(archive);
        rapport.setId(sauvegarde.getId());

        for (Role role : List.of(Role.DIRECTEUR, Role.VICE_DIRECTEUR, Role.ADMIN)) {
            membreRepository.findByRoleAndCompteActif(role, true).stream()
                    .filter(destinataire -> !Objects.equals(destinataire.getId(), membre.getId()))
                    .forEach(destinataire -> notificationService.creerNotification(
                            destinataire.getId(),
                            "Rapport annuel généré",
                            "Le rapport d'activité " + annee + " a été généré.",
                            TypeNotification.SYSTEME,
                            "RAPPORT_ANNUEL",
                            sauvegarde.getId()));
        }
        return rapport;
    }

    @Transactional(readOnly = true)
    public List<RapportAnnuelDTO> obtenirHistorique(Membre membre) {
        verifierAcces(membre);
        return archiveRepository.findAllByOrderByDateGenerationDesc().stream()
                .map(this::versResume)
                .toList();
    }

    @Transactional(readOnly = true)
    public RapportAnnuelDTO obtenirArchive(Long id, Membre membre) {
        verifierAcces(membre);
        RapportAnnuelArchive archive = archiveRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rapport archivé introuvable."));
        try {
            RapportAnnuelDTO rapport = objectMapper.readValue(archive.getDonneesJson(), RapportAnnuelDTO.class);
            rapport.setId(archive.getId());
            return rapport;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("L'instantané du rapport archivé est illisible.", exception);
        }
    }

    private RapportAnnuelDTO versResume(RapportAnnuelArchive archive) {
        return RapportAnnuelDTO.builder()
                .id(archive.getId())
                .annee(archive.getAnnee())
                .version(archive.getVersion())
                .dateGeneration(archive.getDateGeneration())
                .generePar(archive.getGenerePar().getPrenom() + " " + archive.getGenerePar().getNom())
                .build();
    }

    private String serialiser(RapportAnnuelDTO rapport) {
        try {
            return objectMapper.writeValueAsString(rapport);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Impossible d'archiver le rapport généré.", exception);
        }
    }

    private void verifierAcces(Membre membre) {
        if (!authorizationService.estSupervisionPourEvenements(membre)) {
            throw new AccessDeniedException("Le rapport annuel est réservé à la direction et à l'administration.");
        }
    }

    private <E extends Enum<E>> Map<String, Long> comptesEnum(E[] valeurs, List<E> donnees) {
        Map<E, Long> comptes = donnees.stream().filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), () -> new EnumMap<>(valeurs[0].getDeclaringClass()),
                        Collectors.counting()));
        Map<String, Long> resultat = new LinkedHashMap<>();
        for (E valeur : valeurs) {
            resultat.put(valeur.name(), comptes.getOrDefault(valeur, 0L));
        }
        return resultat;
    }

    private Map<String, Long> comptesEquipes(List<Publication> publications) {
        Map<String, Long> resultat = new LinkedHashMap<>();
        for (EquipeLaboratoire equipe : EquipeLaboratoire.values()) {
            long total = publications.stream()
                    .filter(publication -> publication.getAuteurs() != null
                            && publication.getAuteurs().stream()
                            .anyMatch(auteur -> auteur.getEquipeLaboratoire() == equipe))
                    .count();
            resultat.put(equipe.name(), total);
        }
        return resultat;
    }
}
