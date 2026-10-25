package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.dto.MandatClotureRequest;
import com.lias.gestion_laboratoire.dto.MandatRequest;
import com.lias.gestion_laboratoire.dto.MandatResponse;
import com.lias.gestion_laboratoire.dto.MandatUpdateRequest;
import com.lias.gestion_laboratoire.dto.HistoriqueRoleResponse;
import com.lias.gestion_laboratoire.models.HistoriqueRole;
import com.lias.gestion_laboratoire.models.Mandat;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.RoleUtilisateur;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.repositories.HistoriqueRoleRepository;
import com.lias.gestion_laboratoire.repositories.MandatRepository;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class MandatService {
    private final MandatRepository mandatRepository;
    private final MembreRepository membreRepository;
    private final HistoriqueRoleRepository historiqueRoleRepository;
    private final NotificationService notificationService;

    public MandatResponse assignerNouveauMandat(MandatRequest request) {
        Membre auteur = membreConnecte();
        verifierGestion(auteur);
        Membre titulaire = membreRepository.findById(request.getMembreId())
                .orElseThrow(() -> new EntityNotFoundException("Membre introuvable : " + request.getMembreId()));
        verifierTitulaire(titulaire, request.getResponsabilite(), request.getEquipe());
        verifierDates(request.getDateDebut());

        LocalDate dateDebut = request.getDateDebut();
        for (Mandat precedent : obtenirMandatsSlot(request.getResponsabilite(), request.getEquipe())) {
            boolean chevauchement = precedent.getDateFin() == null
                    || !precedent.getDateFin().isBefore(dateDebut);
            if (!chevauchement) continue;
            if (precedent.getDateFin() != null || !precedent.getDateDebut().isBefore(dateDebut)) {
                if (precedent.getTitulaire().getId().equals(titulaire.getId())
                        && precedent.getDateDebut().equals(dateDebut)) {
                    throw new IllegalArgumentException("Ce mandat existe déjà à cette date.");
                }
                throw new IllegalArgumentException("Le nouveau mandat chevauche un mandat déjà enregistré.");
            }
            cloturer(precedent, dateDebut.minusDays(1), auteur,
                    "Passation automatique vers le nouveau titulaire.");
            if (!precedent.getTitulaire().getId().equals(titulaire.getId())) {
                synchroniserRole(precedent.getTitulaire(), auteur, "Clôture par passation de mandat.");
            }
            notifierCloture(precedent);
        }

        Mandat mandat = new Mandat();
        mandat.setTitulaire(titulaire);
        mandat.setResponsabilite(request.getResponsabilite());
        mandat.setEquipe(request.getResponsabilite() == Role.CHEF_EQUIPE ? request.getEquipe() : null);
        mandat.setDateDebut(dateDebut);
        mandat.setDescription(request.getDescription());
        mandat.setAttribuePar(auteur);
        mandat = mandatRepository.save(mandat);
        synchroniserRole(titulaire, auteur, "Attribution du mandat " + mandat.getResponsabilite() + ".");

        notificationService.creerNotification(titulaire.getId(), "Nouveau mandat",
                "Le mandat " + mandat.getResponsabilite() + " vous a été attribué à partir du " + dateDebut + ".");
        if (mandat.getResponsabilite() == Role.DIRECTEUR) {
            notifierLaboratoire("Changement de direction",
                    titulaire.getPrenom() + " " + titulaire.getNom() + " prend la direction du laboratoire.");
        }
        return toResponse(mandat);
    }

    public MandatResponse modifierMandat(Long mandatId, MandatUpdateRequest request) {
        Membre auteur = membreConnecte();
        verifierGestion(auteur);
        Mandat mandat = trouverMandat(mandatId);
        if (mandat.getDateFin() != null) {
            throw new IllegalArgumentException("Un mandat clôturé ne peut pas être modifié.");
        }
        verifierDates(request.getDateDebut());
        EquipeLaboratoire equipe = mandat.getResponsabilite() == Role.CHEF_EQUIPE
                ? request.getEquipe() : null;
        verifierTitulaire(mandat.getTitulaire(), mandat.getResponsabilite(), equipe);

        if (!mandat.getDateDebut().equals(request.getDateDebut())
                || !Objects.equals(mandat.getEquipe(), equipe)) {
            for (Mandat autre : obtenirMandatsSlot(mandat.getResponsabilite(), equipe)) {
                if (!autre.getId().equals(mandatId)
                        && (autre.getDateFin() == null
                        || !autre.getDateFin().isBefore(request.getDateDebut()))) {
                    throw new IllegalArgumentException("La correction créerait un chevauchement de mandats.");
                }
            }
        }
        mandat.setDateDebut(request.getDateDebut());
        mandat.setEquipe(equipe);
        mandat.setDescription(request.getDescription());
        synchroniserRole(mandat.getTitulaire(), auteur, "Correction du mandat " + mandat.getId() + ".");
        return toResponse(mandatRepository.save(mandat));
    }

    public MandatResponse cloturerMandat(Long mandatId, MandatClotureRequest request) {
        Membre auteur = membreConnecte();
        verifierGestion(auteur);
        Mandat mandat = trouverMandat(mandatId);
        LocalDate dateFin = request.getDateFin();
        if (dateFin.isBefore(mandat.getDateDebut())) {
            throw new IllegalArgumentException("La date de fin ne peut pas précéder la date de début.");
        }
        if (mandat.getDateFin() != null) {
            if (mandat.getDateFin().equals(dateFin) && Objects.equals(mandat.getMotifCloture(), request.getMotif())) {
                return toResponse(mandat);
            }
            throw new IllegalArgumentException("Ce mandat est déjà clôturé.");
        }
        if (mandat.getResponsabilite() == Role.DIRECTEUR
                && !hasSuccessor(mandat, dateFin)) {
            throw new IllegalArgumentException(
                    "Impossible de clôturer le dernier mandat de directeur sans successeur actif.");
        }
        cloturer(mandat, dateFin, auteur, request.getMotif());
        synchroniserRole(mandat.getTitulaire(), auteur, "Clôture du mandat " + mandat.getResponsabilite() + ".");
        notifierCloture(mandat);
        if (mandat.getResponsabilite() == Role.DIRECTEUR) {
            notifierLaboratoire("Fin de mandat de direction",
                    "Le mandat de direction de " + mandat.getTitulaire().getPrenom() + " "
                            + mandat.getTitulaire().getNom() + " a été clôturé.");
        }
        return toResponse(mandatRepository.save(mandat));
    }

    @Transactional(readOnly = true)
    public List<MandatResponse> obtenirMandatsActifs() {
        LocalDate aujourdHui = LocalDate.now();
        verifierMembreActif(membreConnecte());
        return mandatRepository.findMandatsValidesAu(aujourdHui).stream()
                .map(m -> MandatResponse.from(m, aujourdHui)).toList();
    }

    @Transactional(readOnly = true)
    public List<MandatResponse> obtenirTousLesMandats() {
        verifierMembreActif(membreConnecte());
        LocalDate aujourdHui = LocalDate.now();
        return mandatRepository.findAll().stream()
                .sorted(Comparator.comparing(Mandat::getDateDebut).reversed())
                .map(m -> MandatResponse.from(m, aujourdHui)).toList();
    }

    @Transactional(readOnly = true)
    public MandatResponse obtenirMandat(Long id) {
        verifierMembreActif(membreConnecte());
        return toResponse(trouverMandat(id));
    }

    @Transactional(readOnly = true)
    public List<MandatResponse> obtenirHistoriqueMembre(Long membreId) {
        verifierMembreActif(membreConnecte());
        if (!membreRepository.existsById(membreId)) {
            throw new EntityNotFoundException("Membre introuvable : " + membreId);
        }
        LocalDate aujourdHui = LocalDate.now();
        return mandatRepository.findByTitulaire_IdOrderByDateDebutDesc(membreId).stream()
                .map(m -> MandatResponse.from(m, aujourdHui)).toList();
    }

    @Transactional(readOnly = true)
    public List<MandatResponse> obtenirMandatsEquipe(EquipeLaboratoire equipe) {
        verifierMembreActif(membreConnecte());
        if (equipe == null || equipe == EquipeLaboratoire.AUCUNE || equipe == EquipeLaboratoire.DIRECTION) {
            throw new IllegalArgumentException("Une équipe de recherche valide est requise.");
        }
        LocalDate aujourdHui = LocalDate.now();
        return mandatRepository.findAll().stream()
                .filter(m -> m.getResponsabilite() == Role.CHEF_EQUIPE && m.getEquipe() == equipe)
                .sorted(Comparator.comparing(Mandat::getDateDebut).reversed())
                .map(m -> MandatResponse.from(m, aujourdHui)).toList();
    }

    @Transactional(readOnly = true)
    public List<HistoriqueRoleResponse> obtenirHistoriqueRoles(Long membreId) {
        verifierMembreActif(membreConnecte());
        if (!membreRepository.existsById(membreId)) {
            throw new EntityNotFoundException("Membre introuvable : " + membreId);
        }
        return historiqueRoleRepository.findByMembreIdOrderByDateChangementDesc(membreId).stream()
                .map(HistoriqueRoleResponse::from).toList();
    }

    public void cloturerMandatsMembre(Membre membre, String motif) {
        Membre auteur = membreConnecte();
        verifierGestion(auteur);
        LocalDate aujourdhui = LocalDate.now();
        for (Mandat mandat : mandatRepository.findMandatsMembreValidesAu(membre.getId(), aujourdhui)) {
            if (mandat.getResponsabilite() == Role.DIRECTEUR && !hasSuccessor(mandat, aujourdhui)) {
                throw new IllegalArgumentException("Le départ du directeur nécessite d'abord la nomination d'un successeur.");
            }
            cloturer(mandat, aujourdhui, auteur, motif);
            notifierCloture(mandat);
        }
        synchroniserRole(membre, auteur, motif);
    }

    private Mandat trouverMandat(Long id) {
        return mandatRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Aucun mandat trouvé avec l'ID : " + id));
    }

    private MandatResponse toResponse(Mandat mandat) {
        return MandatResponse.from(mandat, LocalDate.now());
    }

    private void verifierGestion(Membre auteur) {
        verifierMembreActif(auteur);
        if (auteur.getRoleSysteme() == RoleUtilisateur.ADMIN) return;
        boolean directeurEnMandat = mandatRepository.findMandatsMembreValidesAu(auteur.getId(), LocalDate.now())
                .stream().anyMatch(m -> m.getResponsabilite() == Role.DIRECTEUR);
        if (!directeurEnMandat) {
            throw new AccessDeniedException("Seul l'administrateur système ou le directeur en mandat peut gérer la gouvernance.");
        }
    }

    private void verifierMembreActif(Membre membre) {
        if (!membre.isCompteActif() || membre.isCompteGele()
                || membre.getStatut() == Statut.ANCIEN || membre.getStatut() == Statut.RETRAITE
                || membre.getStatut() == Statut.DOCTORANT) {
            throw new AccessDeniedException("Un membre inactif ne peut pas accéder à la gouvernance.");
        }
    }

    private void verifierTitulaire(Membre titulaire, Role role, EquipeLaboratoire equipe) {
        verifierMembreActif(titulaire);
        if (titulaire.getStatut() == Statut.DOCTORANT) {
            throw new IllegalArgumentException("Un doctorant ne peut pas recevoir de mandat de gouvernance.");
        }
        if (role == Role.CHEF_EQUIPE) {
            if (equipe == null || equipe == EquipeLaboratoire.DIRECTION || equipe == EquipeLaboratoire.AUCUNE
                    || titulaire.getEquipeLaboratoire() != equipe) {
                throw new IllegalArgumentException("Le chef d'équipe doit appartenir à l'équipe sélectionnée.");
            }
        } else if (role != Role.DIRECTEUR && role != Role.VICE_DIRECTEUR) {
            throw new IllegalArgumentException("Seuls les mandats de direction, vice-direction ou chef d'équipe sont attribuables ici.");
        }
    }

    private void verifierDates(LocalDate dateDebut) {
        if (dateDebut == null || dateDebut.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date de début doit être aujourd'hui ou dans le passé.");
        }
    }

    private List<Mandat> obtenirMandatsSlot(Role role, EquipeLaboratoire equipe) {
        return mandatRepository.findAll().stream()
                .filter(m -> m.getResponsabilite() == role
                        && (role != Role.CHEF_EQUIPE || m.getEquipe() == equipe))
                .toList();
    }

    private boolean hasSuccessor(Mandat mandat, LocalDate dateFin) {
        return mandatRepository.findMandatsRoleValidesAu(Role.DIRECTEUR, dateFin).stream()
                .anyMatch(m -> !m.getId().equals(mandat.getId()));
    }

    private void cloturer(Mandat mandat, LocalDate dateFin, Membre auteur, String motif) {
        mandat.setDateFin(dateFin);
        mandat.setCloturePar(auteur);
        mandat.setDateCloture(LocalDateTime.now());
        mandat.setMotifCloture(motif);
        mandatRepository.save(mandat);
    }

    private void synchroniserRole(Membre membre, Membre auteur, String motif) {
        Role ancienRole = membre.getRole();
        EquipeLaboratoire ancienneEquipe = membre.getEquipeLaboratoire();
        List<Mandat> actifs = mandatRepository.findMandatsMembreValidesAu(membre.getId(), LocalDate.now());
        Role nouveauRole = actifs.stream().map(Mandat::getResponsabilite)
                .filter(r -> r == Role.DIRECTEUR || r == Role.VICE_DIRECTEUR || r == Role.CHEF_EQUIPE)
                .findFirst().orElse(membre.getRoleSysteme() == RoleUtilisateur.ADMIN ? Role.ADMIN : Role.MEMBRE_EQUIPE);
        membre.setRole(nouveauRole);
        actifs.stream().filter(m -> m.getResponsabilite() == Role.CHEF_EQUIPE)
                .map(Mandat::getEquipe).filter(Objects::nonNull).findFirst()
                .ifPresent(membre::setEquipeLaboratoire);
        membreRepository.save(membre);

        if (ancienRole != nouveauRole || ancienneEquipe != membre.getEquipeLaboratoire()) {
            HistoriqueRole historique = new HistoriqueRole();
            historique.setMembre(membre);
            historique.setAncienRole(ancienRole);
            historique.setNouveauRole(nouveauRole);
            historique.setAncienneEquipe(ancienneEquipe);
            historique.setNouvelleEquipe(membre.getEquipeLaboratoire());
            historique.setModifiePar(auteur);
            historique.setMotif(motif);
            historiqueRoleRepository.save(historique);
        }
    }

    private Membre membreConnecte() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Membre membre)) {
            throw new AccessDeniedException("Une authentification membre est requise.");
        }
        return membreRepository.findById(membre.getId())
                .orElseThrow(() -> new EntityNotFoundException("Membre connecté introuvable."));
    }

    private void notifierCloture(Mandat mandat) {
        notificationService.creerNotification(
                mandat.getTitulaire().getId(), "Mandat clôturé",
                "Votre mandat " + mandat.getResponsabilite() + " a été clôturé. Motif : " + mandat.getMotifCloture(),
                com.lias.gestion_laboratoire.models.enums.TypeNotification.MANDAT,
                "MANDAT",
                mandat.getId());
    }

    private void notifierLaboratoire(String titre, String contenu) {
        membreRepository.findByCompteActif(true).stream()
                .filter(m -> m.getStatut() != Statut.ANCIEN && m.getStatut() != Statut.RETRAITE)
                .forEach(m -> notificationService.creerNotification(
                        m.getId(), titre, contenu,
                        com.lias.gestion_laboratoire.models.enums.TypeNotification.MANDAT,
                        "MANDAT",
                        null));
    }
}
