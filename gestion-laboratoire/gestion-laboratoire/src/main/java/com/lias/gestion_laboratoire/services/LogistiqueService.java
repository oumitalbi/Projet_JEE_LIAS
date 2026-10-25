package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.authorization.AuthorizationService;
import com.lias.gestion_laboratoire.dto.*;
import com.lias.gestion_laboratoire.models.*;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.RoleUtilisateur;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.models.enums.StatutDemande;
import com.lias.gestion_laboratoire.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class LogistiqueService {
    private final MaterielRepository materielRepository;
    private final DemandeMaterielRepository demandeMaterielRepository;
    private final ArrivageMaterielRepository arrivageRepository;
    private final MembreRepository membreRepository;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;

    public MaterielResponse ajouterMateriel(MaterielRequest request) {
        Membre actor = currentMember();
        authorizationService.verifierGestionLogistique(actor);
        if (materielRepository.existsByNumeroInventaireIgnoreCase(request.numeroInventaire().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce numéro d'inventaire existe déjà.");
        }
        Materiel item = new Materiel();
        item.setNom(request.nom().trim());
        item.setDescription(request.description());
        item.setNumeroInventaire(request.numeroInventaire().trim());
        item.setCategorie(request.categorie().trim());
        item.setEtat(request.etat());
        item.setDateAcquisition(request.dateAcquisition());
        item.setQuantiteTotale(request.quantiteInitiale());
        item.setQuantiteDisponible(request.quantiteInitiale());
        item.setConsommable(request.consommable());
        item.setCreePar(actor);
        Materiel saved = materielRepository.save(item);
        notifyMembers(actor, "Nouveau matériel",
                "Le matériel « " + saved.getNom() + " » a été ajouté à l'inventaire.");
        return MaterielResponse.from(saved);
    }

    public MaterielResponse modifierMateriel(Long id, MaterielRequest request) {
        Membre actor = currentMember();
        authorizationService.verifierGestionLogistique(actor);
        Materiel item = requireMateriel(id);
        if (item.isArchivee()) throw conflict("Un matériel archivé ne peut pas être modifié.");
        if (!item.getNumeroInventaire().equalsIgnoreCase(request.numeroInventaire().trim())
                && materielRepository.existsByNumeroInventaireIgnoreCase(request.numeroInventaire().trim())) {
            throw conflict("Ce numéro d'inventaire existe déjà.");
        }
        int allocated = item.getQuantiteTotale() - item.getQuantiteDisponible();
        if (allocated > 0 && item.isConsommable() != request.consommable()) {
            throw conflict("Le type prêt/consommable ne peut pas changer tant qu'une quantité est en cours d'utilisation.");
        }
        if (request.quantiteInitiale() < allocated) {
            throw conflict("La quantité totale ne peut pas être inférieure aux unités actuellement prêtées.");
        }
        item.setNom(request.nom().trim());
        item.setDescription(request.description());
        item.setNumeroInventaire(request.numeroInventaire().trim());
        item.setCategorie(request.categorie().trim());
        item.setEtat(request.etat());
        item.setDateAcquisition(request.dateAcquisition());
        item.setQuantiteTotale(request.quantiteInitiale());
        item.setQuantiteDisponible(request.quantiteInitiale() - allocated);
        item.setConsommable(request.consommable());
        item.setModifiePar(actor);
        item.setDateModification(LocalDateTime.now());
        return MaterielResponse.from(materielRepository.save(item));
    }

    public MaterielResponse archiverMateriel(Long id, boolean archive) {
        Membre actor = currentMember();
        authorizationService.verifierGestionLogistique(actor);
        Materiel item = requireMateriel(id);
        if (archive && item.getQuantiteDisponible() != item.getQuantiteTotale()) {
            throw conflict("Un matériel affecté ou consommé ne peut pas être archivé.");
        }
        item.setArchivee(archive);
        if (archive) {
            item.setArchivePar(actor);
            item.setDateArchivage(LocalDateTime.now());
        } else {
            item.setRestaurePar(actor);
            item.setDateRestauration(LocalDateTime.now());
        }
        item.setModifiePar(actor);
        item.setDateModification(LocalDateTime.now());
        return MaterielResponse.from(materielRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<MaterielResponse> obtenirMateriel(boolean archives, boolean disponiblesSeulement) {
        authorizationService.verifierAccesLogistique(currentMember());
        return materielRepository.findAllByOrderByCategorieAscNomAsc().stream()
                .filter(item -> archives || !item.isArchivee())
                .filter(item -> !disponiblesSeulement
                        || (!item.isArchivee() && item.getQuantiteDisponible() != null
                        && item.getQuantiteDisponible() > 0 && item.getEtat() != null
                        && !item.getEtat().name().equals("EN_PANNE")
                        && !item.getEtat().name().equals("OBSOLETE")))
                .map(MaterielResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MaterielResponse obtenirMateriel(Long id) {
        authorizationService.verifierAccesLogistique(currentMember());
        return MaterielResponse.from(requireMateriel(id));
    }

    public DemandeMaterielResponse soumettreDemande(DemandeMaterielRequest request) {
        Membre requester = currentMember();
        authorizationService.verifierSoumissionDemandeLogistique(requester);
        Materiel requestedItem = null;
        String itemName = request.nomBesoin() == null ? "" : request.nomBesoin().trim();
        if (request.materielId() != null) {
            requestedItem = requireMateriel(request.materielId());
            if (requestedItem.isArchivee()) throw conflict("Ce matériel n'est plus disponible au catalogue.");
            if (requestedItem.getQuantiteDisponible() < request.quantite()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La quantité demandée dépasse le stock actuellement disponible.");
            }
            if (requestedItem.getEtat() == null || requestedItem.getEtat().name().equals("EN_PANNE")
                    || requestedItem.getEtat().name().equals("OBSOLETE")) {
                throw conflict("Ce matériel est en panne ou obsolète.");
            }
            itemName = requestedItem.getNom();
        } else if (itemName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choisissez un matériel du catalogue ou précisez le besoin hors catalogue.");
        }
        DemandeMateriel demand = new DemandeMateriel();
        demand.setDemandeur(requester);
        demand.setMaterielDemande(requestedItem);
        demand.setNomBesoin(itemName);
        demand.setQuantiteDemandee(request.quantite());
        demand.setMotifDemande(request.justification().trim());
        demand.setDescriptionBesoin(request.justification().trim());
        demand.setStatut(StatutDemande.EN_ATTENTE);
        DemandeMateriel saved = demandeMaterielRepository.save(demand);
        notifyDirectors(requester, "Nouvelle demande de matériel",
                requester.getPrenom() + " " + requester.getNom() + " demande "
                        + request.quantite() + " unité(s) de « " + itemName + " ».");
        return toResponse(saved, requester);
    }

    @Transactional(readOnly = true)
    public List<DemandeMaterielResponse> listerDemandes(boolean mesDemandes, StatutDemande statut) {
        Membre actor = currentMember();
        authorizationService.verifierAccesLogistique(actor);
        if (!mesDemandes && !authorizationService.peutGererLogistique(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La file complète des demandes est réservée au directeur.");
        }
        List<DemandeMateriel> demands = mesDemandes
                ? demandeMaterielRepository.findByDemandeurIdOrderByDateDemandeDesc(actor.getId())
                : demandeMaterielRepository.findAllByOrderByDateDemandeDesc();
        return demands.stream().filter(demand -> statut == null || demand.getStatut() == statut)
                .map(demand -> toResponse(demand, actor)).toList();
    }

    @Transactional(readOnly = true)
    public DemandeMaterielResponse obtenirDemande(Long id) {
        Membre actor = currentMember();
        DemandeMateriel demand = requireDemande(id);
        authorizationService.verifierConsultationDemandeLogistique(actor, demand);
        return toResponse(demand, actor);
    }

    public DemandeMaterielResponse validerDemande(Long demandeId, Long materielId) {
        Membre actor = currentMember();
        authorizationService.verifierGestionLogistique(actor);
        DemandeMateriel demand = requireDemandeForUpdate(demandeId);
        if (demand.getStatut() == StatutDemande.VALIDEE) return toResponse(demand, actor);
        if (demand.getStatut() != StatutDemande.EN_ATTENTE) {
            throw conflict("Seule une demande en attente peut être validée.");
        }
        Long selectedId = materielId != null ? materielId
                : demand.getMaterielDemande() == null ? null : demand.getMaterielDemande().getId();
        if (demand.getMaterielDemande() != null && selectedId != null
                && !Objects.equals(demand.getMaterielDemande().getId(), selectedId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La validation doit porter sur le matériel initialement demandé.");
        }
        if (selectedId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sélectionnez un matériel du catalogue pour satisfaire cette demande hors catalogue.");
        }
        Materiel item = materielRepository.findByIdForUpdate(selectedId)
                .orElseThrow(() -> new EntityNotFoundException("Matériel introuvable : " + selectedId));
        if (item.isArchivee() || item.getEtat() == null
                || item.getEtat().name().equals("EN_PANNE") || item.getEtat().name().equals("OBSOLETE")) {
            throw conflict("Ce matériel est archivé, en panne ou obsolète.");
        }
        int quantity = demand.getQuantiteDemandee();
        if (item.getQuantiteDisponible() < quantity) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le stock disponible a changé et ne permet plus de satisfaire la demande.");
        }
        item.setQuantiteDisponible(item.getQuantiteDisponible() - quantity);
        if (item.isConsommable()) item.setQuantiteTotale(item.getQuantiteTotale() - quantity);
        demand.setMaterielAssigne(item);
        demand.setStatut(StatutDemande.VALIDEE);
        demand.setDateReponse(LocalDateTime.now());
        demand.setTraitePar(actor);
        materielRepository.save(item);
        DemandeMateriel saved = demandeMaterielRepository.save(demand);
        notificationService.creerNotification(
                demand.getDemandeur().getId(),
                "Demande de matériel validée",
                "Votre demande de « " + item.getNom() + " » a été validée.",
                com.lias.gestion_laboratoire.models.enums.TypeNotification.LOGISTIQUE,
                "DEMANDE_MATERIEL",
                saved.getId());
        return toResponse(saved, actor);
    }

    public DemandeMaterielResponse refuserDemande(Long demandeId, String motif) {
        Membre actor = currentMember();
        authorizationService.verifierGestionLogistique(actor);
        DemandeMateriel demand = requireDemandeForUpdate(demandeId);
        if (demand.getStatut() == StatutDemande.REFUSEE) return toResponse(demand, actor);
        if (demand.getStatut() != StatutDemande.EN_ATTENTE) {
            throw conflict("Seule une demande en attente peut être refusée.");
        }
        if (motif == null || motif.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le motif du refus est obligatoire.");
        }
        demand.setStatut(StatutDemande.REFUSEE);
        demand.setMotifRefus(motif.trim());
        demand.setDateReponse(LocalDateTime.now());
        demand.setTraitePar(actor);
        DemandeMateriel saved = demandeMaterielRepository.save(demand);
        notificationService.creerNotification(
                demand.getDemandeur().getId(),
                "Demande de matériel refusée",
                "Motif : " + demand.getMotifRefus(),
                com.lias.gestion_laboratoire.models.enums.TypeNotification.LOGISTIQUE,
                "DEMANDE_MATERIEL",
                saved.getId());
        return toResponse(saved, actor);
    }

    public DemandeMaterielResponse retournerMateriel(Long demandeId) {
        Membre actor = currentMember();
        authorizationService.verifierAccesLogistique(actor);
        DemandeMateriel demand = requireDemandeForUpdate(demandeId);
        authorizationService.verifierConsultationDemandeLogistique(actor, demand);
        if (demand.getStatut() == StatutDemande.RETOURNEE) return toResponse(demand, actor);
        if (demand.getStatut() != StatutDemande.VALIDEE && demand.getStatut() != StatutDemande.LIVREE) {
            throw conflict("Seule une demande validée peut être retournée.");
        }
        Materiel requested = demand.getMaterielAssigne();
        if (requested == null || requested.isConsommable()) {
            throw conflict("Cette demande concerne un consommable, aucun retour n'est attendu.");
        }
        Materiel item = materielRepository.findByIdForUpdate(requested.getId())
                .orElseThrow(() -> new EntityNotFoundException("Matériel introuvable : " + requested.getId()));
        item.setQuantiteDisponible(Math.min(item.getQuantiteTotale(), item.getQuantiteDisponible()
                + demand.getQuantiteDemandee()));
        demand.setQuantiteRetournee(demand.getQuantiteDemandee());
        demand.setRetournePar(actor);
        demand.setDateRetour(LocalDateTime.now());
        demand.setStatut(StatutDemande.RETOURNEE);
        materielRepository.save(item);
        DemandeMateriel saved = demandeMaterielRepository.save(demand);
        return toResponse(saved, actor);
    }

    public ArrivageMaterielResponse enregistrerArrivage(ArrivageMaterielRequest request) {
        Membre actor = currentMember();
        authorizationService.verifierGestionLogistique(actor);
        Materiel item = materielRepository.findByIdForUpdate(request.materielId())
                .orElseThrow(() -> new EntityNotFoundException("Matériel introuvable : " + request.materielId()));
        if (item.isArchivee()) throw conflict("Un arrivage ne peut pas être ajouté à un matériel archivé.");
        if (request.bonLivraison() != null && !request.bonLivraison().isBlank()
                && arrivageRepository.existsByMaterielIdAndBonLivraisonIgnoreCase(
                        item.getId(), request.bonLivraison().trim())) {
            throw conflict("Ce bon de livraison est déjà enregistré pour ce matériel.");
        }
        item.setQuantiteTotale(item.getQuantiteTotale() + request.quantite());
        item.setQuantiteDisponible(item.getQuantiteDisponible() + request.quantite());
        materielRepository.save(item);
        ArrivageMateriel arrival = new ArrivageMateriel();
        arrival.setMateriel(item);
        arrival.setQuantite(request.quantite());
        arrival.setDateArrivage(request.dateArrivage());
        arrival.setFournisseur(request.fournisseur());
        arrival.setBonLivraison(request.bonLivraison());
        arrival.setEnregistrePar(actor);
        return ArrivageMaterielResponse.from(arrivageRepository.save(arrival));
    }

    @Transactional(readOnly = true)
    public List<ArrivageMaterielResponse> listerArrivages(Long materielId) {
        Membre actor = currentMember();
        authorizationService.verifierAccesLogistique(actor);
        List<ArrivageMateriel> arrivals = materielId == null
                ? arrivageRepository.findAllByOrderByDateArrivageDesc()
                : arrivageRepository.findByMaterielIdOrderByDateArrivageDesc(materielId);
        return arrivals.stream().map(ArrivageMaterielResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DistributionEquipeResponse> indicateursEquite() {
        Membre actor = currentMember();
        authorizationService.verifierGestionLogistique(actor);
        java.util.Map<Long, Integer> received = demandeMaterielRepository.findAllByOrderByDateDemandeDesc().stream()
                .filter(request -> request.getStatut() == StatutDemande.VALIDEE
                        || request.getStatut() == StatutDemande.LIVREE
                        || request.getStatut() == StatutDemande.RETOURNEE)
                .filter(request -> request.getDemandeur() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        request -> request.getDemandeur().getId(),
                        java.util.stream.Collectors.summingInt(DemandeMateriel::getQuantiteDemandee)));
        return membreRepository.findAll().stream()
                .filter(member -> member.isCompteActif() && !member.isCompteGele())
                .filter(member -> member.getStatut() == Statut.PERMANENT || member.getStatut() == Statut.ASSOCIE)
                .filter(member -> member.getRoleSysteme() != RoleUtilisateur.DOCTORANT)
                .sorted(java.util.Comparator.comparing(Membre::getNom).thenComparing(Membre::getPrenom))
                .map(member -> new DistributionEquipeResponse(member.getId(), member.getNom(), member.getPrenom(),
                        member.getEquipeLaboratoire() == null ? "AUCUNE" : member.getEquipeLaboratoire().name(),
                        received.getOrDefault(member.getId(), 0)))
                .toList();
    }

    private DemandeMaterielResponse toResponse(DemandeMateriel demand, Membre actor) {
        return DemandeMaterielResponse.from(demand, authorizationService.peutGererLogistique(actor),
                authorizationService.peutRetournerDemandeLogistique(actor, demand)
                        && (demand.getStatut() == StatutDemande.VALIDEE
                        || demand.getStatut() == StatutDemande.LIVREE)
                        && demand.getMaterielAssigne() != null && !demand.getMaterielAssigne().isConsommable());
    }

    private void notifyDirectors(Membre actor, String title, String content) {
        notifyDirectors(actor, title, content, null);
    }

    private void notifyDirectors(Membre actor, String title, String content, Long demandId) {
        membreRepository.findAll().stream()
                .filter(member -> member.isCompteActif() && !member.isCompteGele())
                .filter(member -> member.getStatut() != Statut.RETRAITE && member.getStatut() != Statut.ANCIEN)
                .filter(member -> member.getRoleSysteme() != RoleUtilisateur.DOCTORANT)
                .filter(member -> member.getRole() == Role.DIRECTEUR
                        || member.getRoleSysteme() == RoleUtilisateur.DIRECTEUR)
                .filter(member -> !Objects.equals(member.getId(), actor.getId()))
                .map(Membre::getId).distinct()
                .forEach(id -> notificationService.creerNotification(
                        id, title, content,
                        com.lias.gestion_laboratoire.models.enums.TypeNotification.LOGISTIQUE,
                        "DEMANDE_MATERIEL", demandId));
    }

    private void notifyMembers(Membre actor, String title, String content) {
        notifyMembers(actor, title, content, null);
    }

    private void notifyMembers(Membre actor, String title, String content, Long materielId) {
        membreRepository.findAll().stream()
                .filter(member -> member.isCompteActif() && !member.isCompteGele())
                .filter(member -> member.getStatut() == Statut.PERMANENT || member.getStatut() == Statut.ASSOCIE)
                .filter(member -> member.getRoleSysteme() != RoleUtilisateur.DOCTORANT)
                .filter(member -> !Objects.equals(member.getId(), actor.getId()))
                .map(Membre::getId).distinct()
                .forEach(id -> notificationService.creerNotification(
                        id, title, content,
                        com.lias.gestion_laboratoire.models.enums.TypeNotification.LOGISTIQUE,
                        "MATERIEL", materielId));
    }

    private Membre currentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (!(principal instanceof Membre member)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un membre authentifié est requis.");
        }
        return member;
    }

    private Materiel requireMateriel(Long id) {
        return materielRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Matériel introuvable : " + id));
    }

    private DemandeMateriel requireDemande(Long id) {
        return demandeMaterielRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable : " + id));
    }

    private DemandeMateriel requireDemandeForUpdate(Long id) {
        return demandeMaterielRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable : " + id));
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    public record DistributionEquipeResponse(Long membreId, String nom, String prenom,
                                             String equipe, Integer quantiteDistribuee) { }
}
