package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.authorization.AuthorizationService;
import com.lias.gestion_laboratoire.dto.EvenementRequest;
import com.lias.gestion_laboratoire.dto.EvenementResponse;
import com.lias.gestion_laboratoire.dto.EvenementPublicResponse;
import com.lias.gestion_laboratoire.models.Evenement;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.StatutEvenement;
import com.lias.gestion_laboratoire.repositories.EvenementRepository;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class EvenementService {

    private final EvenementRepository evenementRepository;
    private final MembreRepository membreRepository;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<EvenementResponse> obtenirTousLesEvenements(Membre utilisateur) {
        authorizationService.verifierLectureEvenements(utilisateur);
        return evenementRepository.findByEstAnnuleFalseOrderByDateDebutAsc().stream()
                .map(evenement -> convertir(evenement, utilisateur))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvenementResponse> obtenirEvenementsAVenir(Membre utilisateur) {
        authorizationService.verifierLectureEvenements(utilisateur);
        return evenementRepository
                .findByDateDebutAfterAndEstAnnuleFalseOrderByDateDebutAsc(LocalDateTime.now()).stream()
                .map(evenement -> convertir(evenement, utilisateur))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvenementPublicResponse> obtenirEvenementsPublicsAVenir() {
        return evenementRepository
                .findByDateDebutAfterAndEstAnnuleFalseOrderByDateDebutAsc(LocalDateTime.now())
                .stream()
                .limit(8)
                .map(EvenementPublicResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvenementResponse> obtenirEvenementsParType(
            com.lias.gestion_laboratoire.models.enums.TypeEvenement type,
            Membre utilisateur) {
        authorizationService.verifierLectureEvenements(utilisateur);
        return evenementRepository.findByTypeAndEstAnnuleFalseOrderByDateDebutAsc(type).stream()
                .map(evenement -> convertir(evenement, utilisateur))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvenementResponse> obtenirEvenementsParParticipant(Long membreId, Membre utilisateur) {
        authorizationService.verifierLectureEvenements(utilisateur);
        if (!membreId.equals(utilisateur.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous ne pouvez consulter que vos propres événements.");
        }
        return evenementRepository.findByParticipants_IdAndEstAnnuleFalse(membreId).stream()
                .map(evenement -> convertir(evenement, utilisateur))
                .toList();
    }

    @Transactional(readOnly = true)
    public EvenementResponse obtenirEvenement(Long id, Membre utilisateur) {
        authorizationService.verifierLectureEvenements(utilisateur);
        return convertir(trouver(id), utilisateur);
    }

    @Transactional(readOnly = true)
    public List<EvenementResponse> obtenirEvenementsAnnules(Membre utilisateur) {
        authorizationService.verifierGestionOrganisateursAnnules(utilisateur);
        boolean supervision = authorizationService.estSupervisionPourEvenements(utilisateur);
        List<Evenement> annules = supervision
                ? evenementRepository.findByEstAnnuleTrueOrderByDateDebutDesc()
                : evenementRepository.findByOrganisateurs_IdAndEstAnnuleTrueOrderByDateDebutDesc(utilisateur.getId());
        if (!supervision && annules.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seuls les organisateurs d’événements annulés et les responsables du laboratoire peuvent consulter cet historique.");
        }
        return annules.stream()
                .map(evenement -> convertir(evenement, utilisateur))
                .toList();
    }

    public EvenementResponse creerEvenement(EvenementRequest request, Membre createur) {
        authorizationService.verifierCreationEvenement(createur);
        Evenement evenement = new Evenement();
        appliquerChamps(evenement, request);
        evenement.setCreePar(createur);
        evenement.setOrganisateurs(resoudreOrganisateurs(request.getOrganisateursIds()));
        evenement.getOrganisateurs().add(createur);
        evenement.setParticipants(new LinkedHashSet<>());

        Evenement sauvegarde = evenementRepository.save(evenement);
        notifier(sauvegarde, "Nouvel événement",
                "L’événement « " + sauvegarde.getTitre() + " » vient d’être créé.");
        return convertir(sauvegarde, createur);
    }

    public EvenementResponse modifierEvenement(Long id, EvenementRequest request, Membre utilisateur) {
        Evenement evenement = trouver(id);
        authorizationService.verifierGestionEvenement(evenement, utilisateur);

        Set<Long> anciensDestinataires = idsDestinataires(evenement);
        appliquerChamps(evenement, request);
        evenement.setOrganisateurs(resoudreOrganisateurs(request.getOrganisateursIds()));
        if (evenement.getOrganisateurs().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un événement doit conserver au moins un organisateur.");
        }
        evenement.setModifiePar(utilisateur);
        Evenement sauvegarde = evenementRepository.save(evenement);
        notifier(sauvegarde, "Événement modifié",
                "L’événement « " + sauvegarde.getTitre() + " » a été modifié.",
                anciensDestinataires);
        return convertir(sauvegarde, utilisateur);
    }

    public EvenementResponse affecterOrganisateur(Long evenementId, Long membreId, Membre utilisateur) {
        Evenement evenement = trouver(evenementId);
        authorizationService.verifierGestionEvenement(evenement, utilisateur);
        Membre organisateur = trouverMembre(membreId);
        authorizationService.verifierMembreOrganisateurEligible(organisateur);
        evenement.getOrganisateurs().add(organisateur);
        evenement.setModifiePar(utilisateur);
        Evenement sauvegarde = evenementRepository.save(evenement);
        notifier(sauvegarde, "Événement modifié",
                "Un organisateur a été ajouté à l’événement « " + sauvegarde.getTitre() + " ».");
        return convertir(sauvegarde, utilisateur);
    }

    public EvenementResponse retirerOrganisateur(Long evenementId, Long membreId, Membre utilisateur) {
        Evenement evenement = trouver(evenementId);
        authorizationService.verifierGestionEvenement(evenement, utilisateur);
        Membre organisateur = trouverMembre(membreId);
        boolean estOrganisateur = evenement.getOrganisateurs().stream()
                .anyMatch(membre -> membre.getId().equals(organisateur.getId()));
        if (!estOrganisateur) {
            return convertir(evenement, utilisateur);
        }
        if (evenement.getOrganisateurs().size() <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le dernier organisateur ne peut pas être retiré.");
        }
        evenement.getOrganisateurs().removeIf(membre -> membre.getId().equals(organisateur.getId()));
        evenement.setModifiePar(utilisateur);
        Evenement sauvegarde = evenementRepository.save(evenement);
        notifier(sauvegarde, "Événement modifié",
                "La liste des organisateurs de « " + sauvegarde.getTitre() + " » a été modifiée.",
                Set.of(organisateur.getId()));
        return convertir(sauvegarde, utilisateur);
    }

    public EvenementResponse inscrire(Long evenementId, Membre utilisateur) {
        authorizationService.verifierInscriptionEvenement(utilisateur);
        Evenement evenement = trouver(evenementId);
        verifierInscriptionOuDesinscriptionPossible(evenement);
        boolean dejaInscrit = evenement.getParticipants().stream()
                .anyMatch(participant -> participant.getId().equals(utilisateur.getId()));
        if (!dejaInscrit) {
            evenement.getParticipants().add(utilisateur);
            evenementRepository.save(evenement);

            // Notification de confirmation à l'inscrit
            notificationService.creerNotification(
                    utilisateur.getId(),
                    "Confirmation d'inscription",
                    "Vous êtes bien inscrit à l'événement « " + evenement.getTitre() + " ».",
                    com.lias.gestion_laboratoire.models.enums.TypeNotification.EVENEMENT,
                    "EVENEMENT",
                    evenement.getId()
            );

            // Notification aux organisateurs
            if (evenement.getOrganisateurs() != null) {
                for (Membre org : evenement.getOrganisateurs()) {
                    if (!java.util.Objects.equals(org.getId(), utilisateur.getId())) {
                        notificationService.creerNotification(
                                org.getId(),
                                "Nouvelle inscription",
                                utilisateur.getPrenom() + " " + utilisateur.getNom()
                                        + " s'est inscrit à votre événement « " + evenement.getTitre() + " ».",
                                com.lias.gestion_laboratoire.models.enums.TypeNotification.EVENEMENT,
                                "EVENEMENT",
                                evenement.getId()
                        );
                    }
                }
            }
        }
        return convertir(evenement, utilisateur);
    }

    public EvenementResponse desinscrire(Long evenementId, Membre utilisateur) {
        authorizationService.verifierInscriptionEvenement(utilisateur);
        Evenement evenement = trouver(evenementId);
        verifierInscriptionOuDesinscriptionPossible(evenement);
        evenement.getParticipants().removeIf(participant -> participant.getId().equals(utilisateur.getId()));
        evenementRepository.save(evenement);
        return convertir(evenement, utilisateur);
    }

    public EvenementResponse annulerEvenement(Long id, String motif, Membre utilisateur) {
        Evenement evenement = trouver(id);
        authorizationService.verifierGestionEvenement(evenement, utilisateur);
        if (evenement.isEstAnnule()) {
            return convertir(evenement, utilisateur);
        }
        evenement.setEstAnnule(true);
        evenement.setMotifAnnulation(motif.trim());
        evenement.setDateAnnulation(LocalDateTime.now());
        evenement.setAnnulePar(utilisateur);
        evenement.setModifiePar(utilisateur);
        Evenement sauvegarde = evenementRepository.save(evenement);
        notifier(sauvegarde, "Événement annulé",
                "L’événement « " + sauvegarde.getTitre() + " » a été annulé. Motif : " + motif.trim());
        return convertir(sauvegarde, utilisateur);
    }

    private void appliquerChamps(Evenement evenement, EvenementRequest request) {
        evenement.setTitre(request.getTitre().trim());
        evenement.setDescription(request.getDescription());
        evenement.setType(request.getType());
        evenement.setDateDebut(request.getDateDebut());
        evenement.setDateFin(request.getDateFin());
        evenement.setLieu(request.getLieu().trim());
        evenement.setEdition(request.getEdition() == null || request.getEdition().isBlank()
                ? null : request.getEdition().trim());
    }

    private Set<Membre> resoudreOrganisateurs(Set<Long> organisateursIds) {
        Set<Membre> organisateurs = new LinkedHashSet<>();
        for (Long membreId : organisateursIds) {
            Membre membre = trouverMembre(membreId);
            authorizationService.verifierMembreOrganisateurEligible(membre);
            organisateurs.add(membre);
        }
        return organisateurs;
    }

    private void verifierInscriptionOuDesinscriptionPossible(Evenement evenement) {
        if (evenement.isEstAnnule()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L’inscription est impossible : cet événement est annulé.");
        }
        if (evenement.getStatut() == StatutEvenement.PASSE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L’inscription est impossible : cet événement est terminé.");
        }
    }

    private Evenement trouver(Long id) {
        return evenementRepository.findWithParticipantsAndOrganisateursById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucun événement trouvé avec l’ID : " + id));
    }

    private Membre trouverMembre(Long id) {
        return membreRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucun membre trouvé avec l’ID : " + id));
    }

    private EvenementResponse convertir(Evenement evenement, Membre utilisateur) {
        boolean peutGerer = authorizationService.peutGererEvenement(utilisateur, evenement);
        return EvenementResponse.depuis(evenement, utilisateur, peutGerer,
                authorizationService.peutAnnulerEvenement(utilisateur, evenement));
    }

    private Set<Long> idsDestinataires(Evenement evenement) {
        Set<Long> ids = new LinkedHashSet<>();
        evenement.getOrganisateurs().forEach(membre -> ids.add(membre.getId()));
        evenement.getParticipants().forEach(membre -> ids.add(membre.getId()));
        return ids;
    }

    private void notifier(Evenement evenement, String titre, String contenu) {
        notifier(evenement, titre, contenu, Set.of());
    }

    private void notifier(Evenement evenement, String titre, String contenu, Set<Long> autresDestinataires) {
        Set<Long> destinataires = new LinkedHashSet<>(autresDestinataires);
        destinataires.addAll(idsDestinataires(evenement));
        destinataires.forEach(membreId -> notificationService.creerNotification(
                membreId, titre, contenu,
                com.lias.gestion_laboratoire.models.enums.TypeNotification.EVENEMENT,
                "EVENEMENT",
                evenement.getId()));
    }
}
