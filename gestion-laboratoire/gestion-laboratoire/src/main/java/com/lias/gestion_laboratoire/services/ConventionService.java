package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.authorization.AuthorizationService;
import com.lias.gestion_laboratoire.dto.*;
import com.lias.gestion_laboratoire.models.*;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.RoleUtilisateur;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.models.enums.TypeDocument;
import com.lias.gestion_laboratoire.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
public class ConventionService {

    private final PartenaireRepository partenaireRepository;
    private final ConventionRepository conventionRepository;
    private final DocumentRepository documentRepository;
    private final EvenementRepository evenementRepository;
    private final ConventionDocumentHistoriqueRepository documentHistoryRepository;
    private final MembreRepository membreRepository;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;

    public PartenaireResponse ajouterPartenaire(PartenaireRequest request) {
        Membre actor = currentMember();
        authorizationService.verifierGestionPartenaire(actor);
        String name = request.nom().trim();
        if (partenaireRepository.existsByNomIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce partenaire existe déjà.");
        }
        Partenaire partenaire = new Partenaire();
        appliquerPartenaire(partenaire, request);
        return PartenaireResponse.from(partenaireRepository.save(partenaire));
    }

    public PartenaireResponse modifierPartenaire(Long id, PartenaireRequest request) {
        Membre actor = currentMember();
        authorizationService.verifierGestionPartenaire(actor);
        Partenaire partenaire = findPartner(id);
        if (partenaire.isArchive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un partenaire archivé ne peut pas être modifié.");
        }
        if (partenaireRepository.existsByNomIgnoreCaseAndIdNot(request.nom().trim(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un autre partenaire porte déjà ce nom.");
        }
        appliquerPartenaire(partenaire, request);
        return PartenaireResponse.from(partenaireRepository.save(partenaire));
    }

    public PartenaireResponse archiverPartenaire(Long id) {
        Membre actor = currentMember();
        authorizationService.verifierGestionPartenaire(actor);
        Partenaire partenaire = findPartner(id);
        if (!partenaire.isArchive()) {
            partenaire.setArchive(true);
            partenaire.setArchivePar(actor);
            partenaire.setDateArchivage(LocalDateTime.now());
            partenaireRepository.save(partenaire);
        }
        return PartenaireResponse.from(partenaire);
    }

    public PartenaireResponse restaurerPartenaire(Long id) {
        Membre actor = currentMember();
        authorizationService.verifierGestionPartenaire(actor);
        Partenaire partenaire = findPartner(id);
        if (partenaire.isArchive()) {
            partenaire.setArchive(false);
            partenaire.setRestaurePar(actor);
            partenaire.setDateRestauration(LocalDateTime.now());
            partenaireRepository.save(partenaire);
        }
        return PartenaireResponse.from(partenaire);
    }

    @Transactional(readOnly = true)
    public List<PartenaireResponse> listerPartenaires(boolean archives) {
        Membre actor = currentMember();
        authorizationService.verifierAccesConventions(actor);
        if (archives && !authorizationService.peutGererConventions(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La liste des partenaires archivés est réservée à la direction et aux administrateurs.");
        }
        List<Partenaire> partenaires = archives
                ? partenaireRepository.findByArchiveTrueOrderByNomAsc()
                : partenaireRepository.findByArchiveFalseOrderByNomAsc();
        return partenaires.stream().map(PartenaireResponse::from).toList();
    }

    public ConventionResponse creerConvention(ConventionRequest request) {
        Membre actor = currentMember();
        authorizationService.verifierCreationConvention(actor);
        validerPeriode(request);
        Partenaire partenaire = findPartner(request.getPartenaireId());
        if (partenaire.isArchive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Une convention ne peut pas être créée avec un partenaire archivé.");
        }
        if (conventionRepository.existsByTitreIgnoreCaseAndPartenaireId(
                request.getTitre().trim(), partenaire.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une convention portant ce titre existe déjà pour ce partenaire.");
        }
        Convention convention = new Convention();
        appliquerConvention(convention, request);
        convention.setPartenaire(partenaire);
        convention.setCreePar(actor);
        Convention saved = conventionRepository.save(convention);
        notifierSupervision(actor, "Nouvelle convention",
                "La convention « " + saved.getTitre() + " » a été créée.");
        return response(saved, actor);
    }

    public ConventionResponse modifierConvention(Long id, ConventionRequest request) {
        Membre actor = currentMember();
        Convention convention = findConvention(id);
        authorizationService.verifierGestionConvention(actor, convention);
        if (convention.isArchivee()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une convention archivée ne peut pas être modifiée.");
        }
        validerPeriode(request);
        Partenaire partenaire = findPartner(request.getPartenaireId());
        if (partenaire.isArchive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le partenaire sélectionné est archivé.");
        }
        if (conventionRepository.existsByTitreIgnoreCaseAndPartenaireId(
                request.getTitre().trim(), partenaire.getId())
                && !(convention.getTitre().equalsIgnoreCase(request.getTitre().trim())
                && convention.getPartenaire().getId().equals(partenaire.getId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une convention portant ce titre existe déjà pour ce partenaire.");
        }
        appliquerConvention(convention, request);
        convention.setPartenaire(partenaire);
        convention.setModifiePar(actor);
        convention.setDateModification(LocalDateTime.now());
        Convention saved = conventionRepository.save(convention);
        notifierSupervision(actor, "Convention modifiée",
                "La convention « " + saved.getTitre() + " » a été modifiée.");
        return response(saved, actor);
    }

    public ConventionResponse attacherDocument(Long conventionId, Long documentId) {
        Membre actor = currentMember();
        Convention convention = findConvention(conventionId);
        authorizationService.verifierGestionConvention(actor, convention);
        if (convention.isArchivee()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une convention archivée ne peut pas recevoir de document.");
        }
        Document document = documentRepository.findByIdAndEstArchiveFalse(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document actif introuvable : " + documentId));
        if (document.getTypeDocument() != TypeDocument.CONVENTION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le document doit être de type CONVENTION.");
        }
        if (!documentHistoryRepository.existsByConventionIdAndDocumentId(conventionId, documentId)) {
            ConventionDocumentHistorique history = new ConventionDocumentHistorique();
            history.setConvention(convention);
            history.setDocument(document);
            history.setAjoutePar(actor);
            documentHistoryRepository.save(history);
            convention.setDocumentConvention(document);
            convention.setModifiePar(actor);
            convention.setDateModification(LocalDateTime.now());
            conventionRepository.save(convention);
            notifierSupervision(actor, "Document de convention ajouté",
                    "Un document a été associé à la convention « " + convention.getTitre() + " ».");
        }
        return response(convention, actor);
    }

    public ConventionResponse ajouterActivite(Long conventionId, Long evenementId) {
        Membre actor = currentMember();
        Convention convention = findConvention(conventionId);
        authorizationService.verifierGestionConvention(actor, convention);
        if (convention.isArchivee()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une activité ne peut pas être liée à une convention archivée.");
        }
        Evenement evenement = evenementRepository.findById(evenementId)
                .orElseThrow(() -> new EntityNotFoundException("Événement introuvable : " + evenementId));
        if (evenement.isEstAnnule()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un événement annulé ne peut pas être associé comme activité.");
        }
        if (convention.getActivites().add(evenement)) {
            convention.setModifiePar(actor);
            convention.setDateModification(LocalDateTime.now());
            conventionRepository.save(convention);
        }
        return response(convention, actor);
    }

    public ConventionResponse retirerActivite(Long conventionId, Long evenementId) {
        Membre actor = currentMember();
        Convention convention = findConvention(conventionId);
        authorizationService.verifierGestionConvention(actor, convention);
        if (convention.getActivites().removeIf(event -> event.getId().equals(evenementId))) {
            convention.setModifiePar(actor);
            convention.setDateModification(LocalDateTime.now());
            conventionRepository.save(convention);
        }
        return response(convention, actor);
    }

    public ConventionResponse archiverConvention(Long id) {
        Membre actor = currentMember();
        Convention convention = findConvention(id);
        authorizationService.verifierGestionConvention(actor, convention);
        if (!convention.isArchivee()) {
            convention.setArchivee(true);
            convention.setArchivePar(actor);
            convention.setDateArchivage(LocalDateTime.now());
            conventionRepository.save(convention);
            notifierSupervision(actor, "Convention archivée",
                    "La convention « " + convention.getTitre() + " » a été archivée sans suppression.");
        }
        return response(convention, actor);
    }

    public ConventionResponse desarchiverConvention(Long id) {
        Membre actor = currentMember();
        authorizationService.verifierGestionConvention(actor, null);
        Convention convention = findConvention(id);
        if (convention.isArchivee()) {
            convention.setArchivee(false);
            convention.setRestaurePar(actor);
            convention.setDateRestauration(LocalDateTime.now());
            convention.setModifiePar(actor);
            convention.setDateModification(LocalDateTime.now());
            conventionRepository.save(convention);
        }
        return response(convention, actor);
    }

    @Transactional(readOnly = true)
    public List<ConventionResponse> listerConventions(String statut, Long partenaireId, Integer annee, boolean archives) {
        Membre actor = currentMember();
        authorizationService.verifierAccesConventions(actor);
        if (archives && !authorizationService.peutGererConventions(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Les archives sont réservées à la direction et aux administrateurs.");
        }
        List<Convention> conventions = archives
                ? conventionRepository.findByArchiveeTrueOrderByDateExpirationDesc()
                : conventionRepository.findByArchiveeFalseOrderByDateExpirationAsc();
        return conventions.stream()
                .filter(convention -> partenaireId == null
                        || (convention.getPartenaire() != null
                        && partenaireId.equals(convention.getPartenaire().getId())))
                .filter(convention -> annee == null
                        || (convention.getDateSignature() != null
                        && convention.getDateSignature().getYear() == annee))
                .filter(convention -> statut == null || statut.isBlank()
                        || statut.equalsIgnoreCase("TOUS")
                        || conventionStatut(convention).equalsIgnoreCase(statut))
                .map(convention -> response(convention, actor))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConventionResponse> obtenirConventionsActives() {
        return listerConventions("ACTIVE", null, null, false);
    }

    @Transactional(readOnly = true)
    public ConventionResponse obtenirConvention(Long id) {
        Membre actor = currentMember();
        authorizationService.verifierAccesConventions(actor);
        Convention convention = findConvention(id);
        if (convention.isArchivee() && !authorizationService.peutGererConventions(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette convention archivée est réservée à la direction.");
        }
        return response(convention, actor);
    }

    private ConventionResponse response(Convention convention, Membre actor) {
        return ConventionResponse.from(convention, authorizationService.peutGererConventions(actor),
                documentHistoryRepository.findByConventionIdOrderByDateAjoutDesc(convention.getId()));
    }

    private void appliquerPartenaire(Partenaire partenaire, PartenaireRequest request) {
        partenaire.setNom(request.nom().trim());
        partenaire.setType(request.type());
        partenaire.setPays(request.pays() == null ? null : request.pays().trim());
        partenaire.setContactEmail(request.contactEmail() == null ? null : request.contactEmail().trim());
    }

    private void appliquerConvention(Convention convention, ConventionRequest request) {
        convention.setTitre(request.getTitre().trim());
        convention.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        convention.setDateSignature(request.getDateSignature());
        convention.setDateExpiration(request.getDateExpiration());
    }

    private void validerPeriode(ConventionRequest request) {
        if (request.getDateSignature() != null && request.getDateExpiration() != null
                && request.getDateExpiration().isBefore(request.getDateSignature())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date d'expiration doit être égale ou postérieure à la date de signature.");
        }
    }

    private String conventionStatut(Convention convention) {
        if (convention.isArchivee()) return "ARCHIVEE";
        if (convention.getDateSignature() == null || convention.getDateExpiration() == null) return "BROUILLON";
        LocalDate today = LocalDate.now();
        if (today.isBefore(convention.getDateSignature())) return "A_VENIR";
        return convention.getDateExpiration().isBefore(today) ? "EXPIREE" : "ACTIVE";
    }

    private void notifierSupervision(Membre actor, String title, String content) {
        notifierSupervision(actor, title, content, null);
    }

    private void notifierSupervision(Membre actor, String title, String content, Long conventionId) {
        membreRepository.findAll().stream()
                .filter(member -> member.isCompteActif() && !member.isCompteGele())
                .filter(member -> member.getStatut() != Statut.ANCIEN && member.getStatut() != Statut.RETRAITE)
                .filter(member -> member.getId() == null || !member.getId().equals(actor.getId()))
                .filter(member -> member.getRole() == Role.ADMIN
                        || member.getRole() == Role.DIRECTEUR
                        || member.getRole() == Role.VICE_DIRECTEUR
                        || member.getRoleSysteme() == RoleUtilisateur.ADMIN
                        || member.getRoleSysteme() == RoleUtilisateur.DIRECTEUR)
                .forEach(member -> notificationService.creerNotification(
                        member.getId(), title, content,
                        com.lias.gestion_laboratoire.models.enums.TypeNotification.CONVENTION,
                        "CONVENTION", conventionId));
    }

    private Partenaire findPartner(Long id) {
        return partenaireRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Partenaire introuvable : " + id));
    }

    private Convention findConvention(Long id) {
        return conventionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Convention introuvable : " + id));
    }

    private Membre currentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (!(principal instanceof Membre member)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un membre authentifié est requis.");
        }
        return member;
    }
}
