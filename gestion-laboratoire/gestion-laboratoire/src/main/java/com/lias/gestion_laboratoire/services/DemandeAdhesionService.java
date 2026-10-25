package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.dtos.DemandeAdhesionResponse;
import com.lias.gestion_laboratoire.dtos.DemandeCreateRequest;
import com.lias.gestion_laboratoire.dtos.TraitementAdhesionResponse;
import com.lias.gestion_laboratoire.dtos.TraitementRequest;
import com.lias.gestion_laboratoire.models.DemandeAdhesion;
import com.lias.gestion_laboratoire.models.Mandat;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.RoleUtilisateur;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.models.enums.StatutAdhesion;
import com.lias.gestion_laboratoire.repositories.DemandeAdhesionRepository;
import com.lias.gestion_laboratoire.repositories.MandatRepository;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class DemandeAdhesionService {
    private static final Logger log = LoggerFactory.getLogger(DemandeAdhesionService.class);
    private final DemandeAdhesionRepository demandeAdhesionRepository;
    private final MembreRepository membreRepository;
    private final MandatRepository mandatRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;
    private final HistoriqueAffiliationService historiqueAffiliationService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final PrivateAdhesionCvStorage cvStorage;
    private final PlatformTransactionManager transactionManager;

    public DemandeAdhesion soumettreDemande(DemandeCreateRequest request) {
        if (request.getWebsite() != null && !request.getWebsite().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La demande n’a pas pu être soumise.");
        }
        String email = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        if (demandeAdhesionRepository.existsByEmailIgnoreCaseAndStatut(email, StatutAdhesion.EN_ATTENTE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une demande est déjà en attente avec cette adresse email.");
        }
        if (membreRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un compte existe déjà avec cette adresse. Un ancien membre doit demander une réactivation.");
        }
        verifierStatutCandidat(request.getStatutSouhaite());
        verifierEquipe(request.getEquipeSouhaitee());
        Membre directeur = mandatRepository.findMandatsRoleValidesAu(Role.DIRECTEUR, LocalDate.now()).stream()
                .map(Mandat::getTitulaire)
                .filter(membre -> membre.isCompteActif() && !membre.isCompteGele()
                        && (membre.getStatut() == Statut.PERMANENT || membre.getStatut() == Statut.ASSOCIE))
                .findFirst()
                .or(() -> membreRepository.findAll().stream()
                        .filter(m -> m.isCompteActif() && !m.isCompteGele()
                                && (m.getRole() == Role.DIRECTEUR || m.getRoleSysteme() == RoleUtilisateur.DIRECTEUR))
                        .findFirst())
                .orElse(null);
        String cvPath = cvStorage.store(request.getCv());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    cvStorage.delete(cvPath);
                }
            }
        });

        DemandeAdhesion demande = new DemandeAdhesion();
        demande.setNom(request.getNom().trim());
        demande.setPrenom(request.getPrenom().trim());
        demande.setEmail(email);
        demande.setStatutSouhaite(request.getStatutSouhaite());
        demande.setEtablissementOrigine(request.getEtablissementOrigine().trim());
        demande.setLaboratoireOrigine(normaliser(request.getLaboratoireOrigine()));
        demande.setEquipeSouhaitee(request.getEquipeSouhaitee());
        demande.setLettreMotivation(request.getLettreMotivation().trim());
        demande.setCentresInteret(normaliser(request.getCentresInteret()));
        demande.setBiographie(normaliser(request.getBiographie()));
        demande.setCvChemin(cvPath);
        demande.setConsentementDonnees(request.isConsentementDonnees());
        demande.setConsentementDate(LocalDateTime.now());
        demande.setStatut(StatutAdhesion.EN_ATTENTE);
        demande = demandeAdhesionRepository.save(demande);

        if (directeur != null) {
            try {
                notificationService.creerNotification(
                        directeur.getId(),
                        "Nouvelle demande d’adhésion",
                        "Une candidature de " + demande.getPrenom() + " " + demande.getNom() + " attend votre décision.",
                        com.lias.gestion_laboratoire.models.enums.TypeNotification.ADHESION,
                        "ADHESION",
                        demande.getId());
            } catch (Exception e) {
                log.warn("Impossible d'envoyer la notification de candidature au directeur: {}", e.getMessage());
            }
        }
        return demande;
    }

    @Transactional(readOnly = true)
    public List<DemandeAdhesionResponse> obtenirDemandesEnAttente() {
        verifierGestionnaire();
        return demandeAdhesionRepository.findByStatutOrderByDateDemandeDesc(StatutAdhesion.EN_ATTENTE)
                .stream().map(DemandeAdhesionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DemandeAdhesionResponse> obtenirHistorique() {
        verifierGestionnaire();
        return demandeAdhesionRepository.findByStatutNotOrderByDateDemandeDesc(StatutAdhesion.EN_ATTENTE).stream()
                .map(DemandeAdhesionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DemandeAdhesionResponse obtenirDemande(Long demandeId) {
        verifierGestionnaire();
        return DemandeAdhesionResponse.from(trouverDemande(demandeId));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TraitementAdhesionResponse traiterDemande(Long demandeId, TraitementRequest request) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        DecisionEnregistree decision = transaction.execute(status -> enregistrerDecision(demandeId, request));
        if (decision == null) {
            throw new IllegalStateException("La décision d’adhésion n’a pas été enregistrée.");
        }
        if (!decision.notificationRequise()) {
            return new TraitementAdhesionResponse(decision.demande(), false,
                    "Cette demande avait déjà été traitée. Aucune notification supplémentaire n’a été envoyée.");
        }

        try {
            if (decision.acceptee()) {
                passwordResetService.envoyerLienInitialisation(decision.email());
            } else {
                emailService.envoyerRefusAdhesion(decision.email(), decision.motif());
            }
            return new TraitementAdhesionResponse(decision.demande(), true,
                    "La décision a été enregistrée et le candidat a été notifié par e-mail.");
        } catch (Exception exception) {
            log.error("Décision d’adhésion enregistrée, mais l’e-mail n’a pas pu être envoyé. Demande id={}",
                    demandeId, exception);
            return new TraitementAdhesionResponse(decision.demande(), false,
                    "La décision est enregistrée avec succès. Note : l’e-mail de notification n’a pas pu être délivré.");
        }
    }

    private DecisionEnregistree enregistrerDecision(Long demandeId, TraitementRequest request) {
        Membre directeur = verifierGestionnaire();
        DemandeAdhesion demande = trouverDemande(demandeId);
        boolean acceptee = Boolean.TRUE.equals(request.getEstAcceptee());
        StatutAdhesion decision = acceptee ? StatutAdhesion.ACCEPTEE : StatutAdhesion.REFUSEE;
        if (demande.getStatut() != StatutAdhesion.EN_ATTENTE) {
            if (demande.getStatut() == decision) {
                return new DecisionEnregistree(DemandeAdhesionResponse.from(demande), false,
                        acceptee, demande.getEmail(), demande.getMotifDecision());
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande a déjà reçu une décision différente.");
        }

        demande.setTraitePar(directeur);
        demande.setDateTraitement(LocalDateTime.now());
        demande.setMotifDecision(normaliser(request.getMotif()));
        if (!acceptee) {
            if (request.getMotif() == null || request.getMotif().isBlank()) {
                throw new IllegalArgumentException("Un motif est obligatoire pour refuser une demande.");
            }
            demande.setStatut(StatutAdhesion.REFUSEE);
            demande = demandeAdhesionRepository.save(demande);
            return new DecisionEnregistree(DemandeAdhesionResponse.from(demande), true,
                    false, demande.getEmail(), demande.getMotifDecision());
        }

        Statut statutAccorde = request.getStatutAccorde();
        EquipeLaboratoire equipeAccordee = request.getEquipeAccordee();
        verifierStatutCandidat(statutAccorde);
        verifierEquipe(equipeAccordee);
        Role roleInitial = request.getRoleInitial() == null ? Role.MEMBRE_EQUIPE : request.getRoleInitial();
        if (roleInitial != Role.MEMBRE_EQUIPE) {
            throw new IllegalArgumentException(
                    "L’acceptation d’une adhésion ne peut pas attribuer de rôle de gouvernance.");
        }
        if (membreRepository.findByEmailIgnoreCase(demande.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un compte existe déjà avec cette adresse. Aucun nouveau compte n’a été créé.");
        }

        Membre membre = new Membre();
        membre.setNom(demande.getNom());
        membre.setPrenom(demande.getPrenom());
        membre.setEmail(demande.getEmail());
        membre.setTitreAcademique(null);
        membre.setStatut(statutAccorde);
        membre.setRole(Role.MEMBRE_EQUIPE);
        membre.setRoleSysteme(roleUtilisateurPour(statutAccorde));
        membre.setEquipeLaboratoire(equipeAccordee);
        membre.setEtablissementOrigine(demande.getEtablissementOrigine());
        membre.setLaboratoireOrigine(demande.getLaboratoireOrigine());
        membre.setCentresInteret(demande.getCentresInteret());
        membre.setBiographie(demande.getBiographie());
        membre.setDateAffiliation(LocalDate.now());
        membre.setMotDePasse(passwordEncoder.encode(UUID.randomUUID().toString()));
        membre.setCompteActif(true);
        membre.setCompteGele(false);
        membre = membreRepository.save(membre);
        historiqueAffiliationService.creerPremiereAffiliation(membre);

        // Notification de bienvenue au nouveau membre (non bloquante)
        try {
            notificationService.creerNotification(
                    membre.getId(),
                    "Bienvenue au Laboratoire LIAS",
                    "Votre adhésion a été validée. Votre compte membre est désormais opérationnel.",
                    com.lias.gestion_laboratoire.models.enums.TypeNotification.ADHESION,
                    "ADHESION",
                    demande.getId()
            );
        } catch (Exception e) {
            log.warn("Notification de bienvenue non envoyée pour le membre id={} : {}", membre.getId(), e.getMessage());
        }

        demande.setStatut(StatutAdhesion.ACCEPTEE);
        demande.setMembreCree(membre);
        demande = demandeAdhesionRepository.save(demande);
        return new DecisionEnregistree(DemandeAdhesionResponse.from(demande), true,
                true, membre.getEmail(), null);
    }

    @Transactional(readOnly = true)
    public String obtenirCheminCv(Long demandeId) {
        verifierGestionnaire();
        return trouverDemande(demandeId).getCvChemin();
    }

    private DemandeAdhesion trouverDemande(Long demandeId) {
        return demandeAdhesionRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande d’adhésion introuvable : " + demandeId));
    }

    private Membre verifierGestionnaire() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Membre authentifie)) {
            throw new AccessDeniedException("Authentification requise.");
        }
        Membre acteur = membreRepository.findById(authentifie.getId())
                .orElseThrow(() -> new EntityNotFoundException("Membre connecté introuvable."));
        if (!acteur.isCompteActif() || acteur.isCompteGele()
                || acteur.getStatut() == Statut.ANCIEN || acteur.getStatut() == Statut.RETRAITE
                || acteur.getStatut() == Statut.DOCTORANT) {
            throw new AccessDeniedException("Votre compte ne peut pas traiter les adhésions.");
        }
        if (acteur.getRoleSysteme() == RoleUtilisateur.ADMIN || acteur.getRole() == Role.ADMIN) {
            return acteur;
        }
        boolean estDirecteur = acteur.getRole() == Role.DIRECTEUR
                || acteur.getRoleSysteme() == RoleUtilisateur.DIRECTEUR
                || mandatRepository.findMandatsMembreValidesAu(acteur.getId(), LocalDate.now())
                        .stream().anyMatch(mandat -> mandat.getResponsabilite() == Role.DIRECTEUR);
        if (!estDirecteur) {
            throw new AccessDeniedException("Seul le directeur ou l’administrateur peut consulter ou traiter les adhésions.");
        }
        return acteur;
    }

    private void verifierStatutCandidat(Statut statut) {
        if (statut != Statut.PERMANENT && statut != Statut.ASSOCIE && statut != Statut.DOCTORANT) {
            throw new IllegalArgumentException("Le statut doit être permanent, associé ou doctorant.");
        }
    }

    private void verifierEquipe(EquipeLaboratoire equipe) {
        if (equipe == null || equipe == EquipeLaboratoire.AUCUNE || equipe == EquipeLaboratoire.DIRECTION) {
            throw new IllegalArgumentException("Une équipe de recherche valide doit être sélectionnée.");
        }
    }

    private RoleUtilisateur roleUtilisateurPour(Statut statut) {
        return switch (statut) {
            case PERMANENT -> RoleUtilisateur.MEMBRE_PERMANENT;
            case ASSOCIE -> RoleUtilisateur.MEMBRE_ASSOCIE;
            case DOCTORANT -> RoleUtilisateur.DOCTORANT;
            default -> throw new IllegalArgumentException("Statut d’adhésion non admissible.");
        };
    }

    private String normaliser(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record DecisionEnregistree(
            DemandeAdhesionResponse demande,
            boolean notificationRequise,
            boolean acceptee,
            String email,
            String motif) {
    }
}
