package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.authorization.AuthorizationService;
import com.lias.gestion_laboratoire.dto.ReunionRequest;
import com.lias.gestion_laboratoire.dto.ReunionResponse;
import com.lias.gestion_laboratoire.models.Document;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.Reunion;
import com.lias.gestion_laboratoire.models.ReunionProcesVerbal;
import com.lias.gestion_laboratoire.models.enums.StatutReunion;
import com.lias.gestion_laboratoire.models.enums.TypeDocument;
import com.lias.gestion_laboratoire.repositories.DocumentRepository;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import com.lias.gestion_laboratoire.repositories.ReunionProcesVerbalRepository;
import com.lias.gestion_laboratoire.repositories.ReunionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class ReunionService {

    private final ReunionRepository reunionRepository;
    private final MembreRepository membreRepository;
    private final DocumentRepository documentRepository;
    private final ReunionProcesVerbalRepository pvHistoryRepository;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;

    public ReunionResponse planifierReunion(ReunionRequest request) {
        Membre organisateur = currentMember();
        authorizationService.verifierPlanificationReunion(organisateur);
        validerDates(request);
        Reunion reunion = new Reunion();
        appliquerRequest(reunion, request);
        reunion.setOrganisateur(organisateur);
        reunion.setCreePar(organisateur);
        reunion.setStatut(StatutReunion.PLANIFIEE);
        reunion.setDateChangementStatut(LocalDateTime.now());
        reunion.setStatutChangePar(organisateur);
        reunion.getParticipants().add(organisateur);
        ajouterParticipantsEligibles(reunion, request.getParticipantsIds());
        Reunion saved = reunionRepository.save(reunion);
        notifierParticipants(saved, "Convocation à une réunion",
                "Vous êtes invité(e) à la réunion « " + saved.getTitre() + " ».");
        return response(saved, organisateur);
    }

    @Transactional(readOnly = true)
    public List<ReunionResponse> listerReunions(boolean archives) {
        Membre membre = currentMember();
        authorizationService.verifierAccesReunions(membre);
        if (archives && !authorizationService.peutVoirToutesReunions(membre)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La consultation des réunions archivées est réservée à la direction.");
        }
        List<Reunion> reunions;
        if (archives) {
            reunions = reunionRepository.findByArchiveeTrueOrderByDateReunionDesc();
        } else if (authorizationService.peutVoirToutesReunions(membre)) {
            reunions = reunionRepository.findByArchiveeFalseOrderByDateReunionAsc();
        } else {
            reunions = reunionRepository.findByParticipantOrOrganisateur(membre.getId())
                    .stream().filter(reunion -> !reunion.isArchivee()).toList();
        }
        return reunions.stream().map(reunion -> response(reunion, membre)).toList();
    }

    @Transactional(readOnly = true)
    public ReunionResponse obtenirReunion(Long id) {
        Membre membre = currentMember();
        Reunion reunion = findReunion(id);
        authorizationService.verifierConsultationReunion(membre, reunion);
        if (reunion.isArchivee() && !authorizationService.peutVoirToutesReunions(membre)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Cette réunion archivée est réservée à la direction.");
        }
        return response(reunion, membre);
    }

    public ReunionResponse modifierReunion(Long id, ReunionRequest request) {
        Membre membre = currentMember();
        Reunion reunion = findReunion(id);
        authorizationService.verifierGestionReunion(membre, reunion);
        if (reunion.isArchivee()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une réunion archivée ne peut pas être modifiée.");
        }
        if (reunion.getStatut() == StatutReunion.ANNULEE || reunion.getStatut() == StatutReunion.TERMINEE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une réunion terminée ou annulée ne peut pas être modifiée.");
        }
        validerDates(request);
        appliquerRequest(reunion, request);
        Set<Long> requestedParticipants = request.getParticipantsIds();
        if (requestedParticipants == null || requestedParticipants.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins un participant est obligatoire.");
        }
        Set<Long> previousParticipants = reunion.getParticipants().stream()
                .map(Membre::getId).collect(java.util.stream.Collectors.toSet());
        reunion.getParticipants().removeIf(participant -> !participant.getId().equals(reunion.getOrganisateur().getId())
                && !requestedParticipants.contains(participant.getId()));
        ajouterParticipantsEligibles(reunion, requestedParticipants);
        reunion.setModifiePar(membre);
        reunion.setDateModification(LocalDateTime.now());
        Reunion saved = reunionRepository.save(reunion);
        saved.getParticipants().stream()
                .filter(participant -> !previousParticipants.contains(participant.getId()))
                .forEach(participant -> notificationService.creerNotification(
                        participant.getId(), "Convocation à une réunion",
                        "Vous êtes invité(e) à la réunion « " + saved.getTitre() + " »."));
        notifierParticipants(saved, "Réunion modifiée",
                "Les informations de la réunion « " + saved.getTitre() + " » ont été modifiées.");
        return response(saved, membre);
    }

    public ReunionResponse ajouterParticipant(Long reunionId, Long membreId) {
        Membre acteur = currentMember();
        Reunion reunion = findReunion(reunionId);
        authorizationService.verifierGestionReunion(acteur, reunion);
        verifierReunionActive(reunion);
        Membre participant = membreRepository.findById(membreId)
                .orElseThrow(() -> new EntityNotFoundException("Membre introuvable : " + membreId));
        verifierParticipantEligible(participant);
        boolean added = reunion.getParticipants().add(participant);
        if (added) {
            reunion.setModifiePar(acteur);
            reunion.setDateModification(LocalDateTime.now());
            reunionRepository.save(reunion);
            notificationService.creerNotification(
                    participant.getId(), "Convocation à une réunion",
                    "Vous êtes invité(e) à la réunion « " + reunion.getTitre() + " ».");
        }
        return response(reunion, acteur);
    }

    public ReunionResponse retirerParticipant(Long reunionId, Long membreId) {
        Membre acteur = currentMember();
        Reunion reunion = findReunion(reunionId);
        authorizationService.verifierGestionReunion(acteur, reunion);
        verifierReunionActive(reunion);
        if (reunion.getOrganisateur() != null && reunion.getOrganisateur().getId().equals(membreId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'organisateur ne peut pas être retiré des participants.");
        }
        boolean removed = reunion.getParticipants().removeIf(participant -> participant.getId().equals(membreId));
        if (removed) {
            reunion.setModifiePar(acteur);
            reunion.setDateModification(LocalDateTime.now());
            reunionRepository.save(reunion);
        }
        return response(reunion, acteur);
    }

    public ReunionResponse changerStatut(Long reunionId, StatutReunion nouveauStatut) {
        Membre acteur = currentMember();
        Reunion reunion = findReunion(reunionId);
        authorizationService.verifierGestionReunion(acteur, reunion);
        if (nouveauStatut == null || nouveauStatut == StatutReunion.ARCHIVEE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le statut demandé est invalide; utiliser les actions d'archivage dédiées.");
        }
        if (reunion.getStatut() != nouveauStatut) {
            if (reunion.isArchivee() || !transitionPermise(reunion.getStatut(), nouveauStatut)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cette transition de statut n'est pas autorisée.");
            }
            reunion.setStatut(nouveauStatut);
            reunion.setStatutChangePar(acteur);
            reunion.setDateChangementStatut(LocalDateTime.now());
            reunion.setModifiePar(acteur);
            reunion.setDateModification(LocalDateTime.now());
            reunionRepository.save(reunion);
            if (nouveauStatut == StatutReunion.ANNULEE) {
                notifierParticipants(reunion, "Réunion annulée",
                        "La réunion « " + reunion.getTitre() + " » a été annulée.");
            }
        }
        return response(reunion, acteur);
    }

    public ReunionResponse archiver(Long reunionId) {
        Membre acteur = currentMember();
        Reunion reunion = findReunion(reunionId);
        authorizationService.verifierGestionReunion(acteur, reunion);
        if (!reunion.isArchivee()) {
            reunion.setStatutAvantArchivage(reunion.getStatut());
            reunion.setArchivee(true);
            reunion.setStatut(StatutReunion.ARCHIVEE);
            reunion.setStatutChangePar(acteur);
            reunion.setDateChangementStatut(LocalDateTime.now());
            reunion.setModifiePar(acteur);
            reunion.setDateModification(LocalDateTime.now());
            reunionRepository.save(reunion);
        }
        return response(reunion, acteur);
    }

    public ReunionResponse desarchiver(Long reunionId) {
        Membre acteur = currentMember();
        authorizationService.verifierAccesReunions(acteur);
        if (!authorizationService.peutVoirToutesReunions(acteur)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seule la direction peut désarchiver une réunion.");
        }
        Reunion reunion = findReunion(reunionId);
        if (reunion.isArchivee()) {
            reunion.setArchivee(false);
            reunion.setStatut(reunion.getStatutAvantArchivage() == null
                    ? StatutReunion.PLANIFIEE : reunion.getStatutAvantArchivage());
            reunion.setStatutAvantArchivage(null);
            reunion.setStatutChangePar(acteur);
            reunion.setDateChangementStatut(LocalDateTime.now());
            reunion.setModifiePar(acteur);
            reunion.setDateModification(LocalDateTime.now());
            reunionRepository.save(reunion);
        }
        return response(reunion, acteur);
    }

    public ReunionResponse attacherPV(Long reunionId, Long documentId) {
        Membre acteur = currentMember();
        Reunion reunion = findReunion(reunionId);
        authorizationService.verifierGestionReunion(acteur, reunion);
        if (reunion.isArchivee() || (reunion.getStatut() != StatutReunion.EN_COURS
                && reunion.getStatut() != StatutReunion.TERMINEE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un PV peut être attaché uniquement à une réunion en cours ou terminée.");
        }
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document introuvable : " + documentId));
        if (document.getTypeDocument() != TypeDocument.PV) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le document doit être de type PV.");
        }
        if (document.isEstArchive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un document archivé ne peut pas être attaché comme PV.");
        }
        if (reunion.getProcesVerbal() == null
                || !reunion.getProcesVerbal().getId().equals(document.getId())) {
            ReunionProcesVerbal history = new ReunionProcesVerbal();
            history.setReunion(reunion);
            history.setDocument(document);
            history.setAjoutePar(acteur);
            pvHistoryRepository.save(history);
            reunion.setProcesVerbal(document);
            if (reunion.getStatut() != StatutReunion.TERMINEE) {
                reunion.setStatut(StatutReunion.TERMINEE);
                reunion.setStatutChangePar(acteur);
                reunion.setDateChangementStatut(LocalDateTime.now());
            }
            reunion.setModifiePar(acteur);
            reunion.setDateModification(LocalDateTime.now());
            reunionRepository.save(reunion);
            notifierParticipants(reunion, "Procès-verbal disponible",
                    "Un PV a été ajouté à la réunion « " + reunion.getTitre() + " ».");
        }
        return response(reunion, acteur);
    }

    @Transactional(readOnly = true)
    public List<ReunionResponse> obtenirReunionsMembre(Long membreId) {
        Membre acteur = currentMember();
        authorizationService.verifierAccesReunions(acteur);
        if (!acteur.getId().equals(membreId) && !authorizationService.peutVoirToutesReunions(acteur)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous ne pouvez consulter que votre propre agenda.");
        }
        Membre cible = membreRepository.findById(membreId)
                .orElseThrow(() -> new EntityNotFoundException("Membre introuvable : " + membreId));
        authorizationService.verifierAccesReunions(cible);
        return reunionRepository.findByParticipantOrOrganisateur(membreId).stream()
                .filter(reunion -> !reunion.isArchivee())
                .map(reunion -> response(reunion, acteur))
                .toList();
    }

    private ReunionResponse response(Reunion reunion, Membre membre) {
        boolean gestion = authorizationService.peutGererReunion(membre, reunion);
        List<ReunionProcesVerbal> historique = pvHistoryRepository
                .findByReunionIdOrderByDateAjoutDesc(reunion.getId());
        return ReunionResponse.from(reunion, membre, gestion, gestion && !reunion.isArchivee(),
                gestion && !reunion.isArchivee(), historique);
    }

    private void appliquerRequest(Reunion reunion, ReunionRequest request) {
        reunion.setTitre(request.getTitre().trim());
        reunion.setOrdreDuJour(request.getOrdreDuJour().trim());
        reunion.setDateDebut(request.getDateDebut());
        reunion.setDateFin(request.getDateFin());
        reunion.setLieu(request.getLieu().trim());
    }

    private void validerDates(ReunionRequest request) {
        if (request.getDateDebut() != null && request.getDateDebut().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de début ne peut pas être dans le passé.");
        }
        if (request.getDateDebut() != null && request.getDateFin() != null
                && request.getDateFin().isBefore(request.getDateDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de fin doit être postérieure ou égale à la date de début.");
        }
    }

    private void ajouterParticipantsEligibles(Reunion reunion, Set<Long> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins un participant est obligatoire.");
        }
        for (Long membreId : new LinkedHashSet<>(participantIds)) {
            Membre participant = membreRepository.findById(membreId)
                    .orElseThrow(() -> new EntityNotFoundException("Membre introuvable : " + membreId));
            verifierParticipantEligible(participant);
            reunion.getParticipants().add(participant);
        }
    }

    private void verifierParticipantEligible(Membre participant) {
        if (!authorizationService.peutParticiperReunion(participant)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un participant doit être un membre permanent ou associé actif; doctorants et anciens membres sont exclus.");
        }
    }

    private boolean transitionPermise(StatutReunion current, StatutReunion next) {
        return switch (current) {
            case PLANIFIEE -> next == StatutReunion.EN_COURS || next == StatutReunion.ANNULEE;
            case EN_COURS -> next == StatutReunion.TERMINEE || next == StatutReunion.ANNULEE;
            case TERMINEE, ANNULEE, ARCHIVEE -> false;
        };
    }

    private void verifierReunionActive(Reunion reunion) {
        if (reunion.isArchivee() || reunion.getStatut() == StatutReunion.ANNULEE
                || reunion.getStatut() == StatutReunion.TERMINEE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Les participants ne peuvent plus être modifiés pour cette réunion.");
        }
    }

    private Reunion findReunion(Long id) {
        return reunionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Réunion introuvable : " + id));
    }

    private Membre currentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Membre membre)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un membre authentifié est requis.");
        }
        return membre;
    }

    private void notifierParticipants(Reunion reunion, String titre, String contenu) {
        Set<Long> destinataires = new LinkedHashSet<>();
        if (reunion.getParticipants() != null) {
            reunion.getParticipants().forEach(participant -> destinataires.add(participant.getId()));
        }
        if (reunion.getOrganisateur() != null) {
            destinataires.add(reunion.getOrganisateur().getId());
        }
        destinataires.forEach(id -> notificationService.creerNotification(
                id, titre, contenu,
                com.lias.gestion_laboratoire.models.enums.TypeNotification.REUNION,
                "REUNION",
                reunion.getId()));
    }
}
