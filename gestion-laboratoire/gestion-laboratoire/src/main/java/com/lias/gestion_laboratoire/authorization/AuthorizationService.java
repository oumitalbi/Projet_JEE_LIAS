package com.lias.gestion_laboratoire.authorization;

import com.lias.gestion_laboratoire.models.Evenement;
import com.lias.gestion_laboratoire.models.Document;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.Publication;
import com.lias.gestion_laboratoire.models.Reunion;
import com.lias.gestion_laboratoire.models.Convention;
import com.lias.gestion_laboratoire.models.DemandeMateriel;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.RoleUtilisateur;
import com.lias.gestion_laboratoire.models.enums.Statut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class AuthorizationService {

    public void verifierArchivagePublication(Publication publication) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Membre membre) || !peutArchiverPublication(membre, publication, authentication)) {
            throw new ResponseStatusException(
                    FORBIDDEN,
                    "Vous n'êtes pas autorisé à archiver cette publication");
        }
    }

    public void verifierGestionEvenement(Evenement evenement) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();

        verifierGestionEvenement(evenement, principal instanceof Membre membre ? membre : null);
    }

    public void verifierLectureEvenements(Membre membre) {
        verifierMembreActif(membre);
    }

    public void verifierAccesDocuments(Membre membre) {
        verifierMembreActif(membre);
        if (membre.getStatut() == Statut.DOCTORANT) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Les documents internes sont réservés aux membres non doctorants.");
        }
    }

    public boolean peutConsulterDocuments(Membre membre) {
        return estMembreActif(membre) && membre.getStatut() != Statut.DOCTORANT;
    }

    public void verifierAccesReunions(Membre membre) {
        verifierMembreActif(membre);
        if (membre.getStatut() == Statut.DOCTORANT) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Les réunions internes sont réservées aux membres non doctorants.");
        }
    }

    public void verifierAccesConventions(Membre membre) {
        verifierMembreActif(membre);
        if (!estSupervision(membre)
                && membre.getStatut() != Statut.PERMANENT
                && membre.getStatut() != Statut.ASSOCIE) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Les conventions sont accessibles aux membres permanents et associés actifs.");
        }
    }

    public boolean peutGererConventions(Membre membre) {
        return estMembreActif(membre) && estSupervision(membre);
    }

    public void verifierAccesLogistique(Membre membre) {
        verifierMembreActif(membre);
        if ((membre.getStatut() != Statut.PERMANENT && membre.getStatut() != Statut.ASSOCIE)
                || membre.getRoleSysteme() == RoleUtilisateur.DOCTORANT) {
            throw new ResponseStatusException(FORBIDDEN,
                    "La logistique est accessible aux membres permanents et associés actifs.");
        }
    }

    public boolean peutGererLogistique(Membre membre) {
        return estMembreActif(membre)
                && membre.getRole() == Role.DIRECTEUR;
    }

    public void verifierGestionLogistique(Membre membre) {
        verifierAccesLogistique(membre);
        if (!peutGererLogistique(membre)) {
            throw new ResponseStatusException(FORBIDDEN,
                    "La gestion du matériel et des demandes est réservée au directeur.");
        }
    }

    public void verifierSoumissionDemandeLogistique(Membre membre) {
        verifierAccesLogistique(membre);
    }

    public boolean peutConsulterDemandeLogistique(Membre membre, DemandeMateriel demande) {
        return estMembreActif(membre) && demande != null && demande.getDemandeur() != null
                && (peutGererLogistique(membre)
                    || Objects.equals(membre.getId(), demande.getDemandeur().getId()));
    }

    public void verifierConsultationDemandeLogistique(Membre membre, DemandeMateriel demande) {
        verifierAccesLogistique(membre);
        if (!peutConsulterDemandeLogistique(membre, demande)) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Vous ne pouvez consulter que vos demandes.");
        }
    }

    public boolean peutRetournerDemandeLogistique(Membre membre, DemandeMateriel demande) {
        return estMembreActif(membre) && demande != null && demande.getDemandeur() != null
                && (peutGererLogistique(membre)
                    || Objects.equals(membre.getId(), demande.getDemandeur().getId()));
    }

    public void verifierCreationConvention(Membre membre) {
        verifierAccesConventions(membre);
        boolean directeur = membre.getRole() == Role.DIRECTEUR;
        if (!directeur) {
            throw new ResponseStatusException(FORBIDDEN,
                    "La création d'une convention est réservée au directeur du laboratoire.");
        }
    }

    public void verifierGestionConvention(Membre membre, Convention convention) {
        verifierAccesConventions(membre);
        if (!peutGererConventions(membre)) {
            throw new ResponseStatusException(FORBIDDEN,
                    "La gestion des conventions est réservée au directeur, au vice-directeur et aux administrateurs.");
        }
    }

    public void verifierGestionPartenaire(Membre membre) {
        verifierAccesConventions(membre);
        if (!peutGererConventions(membre)) {
            throw new ResponseStatusException(FORBIDDEN,
                    "La gestion des partenaires est réservée au directeur, au vice-directeur et aux administrateurs.");
        }
    }

    public boolean peutPlanifierReunion(Membre membre) {
        if (!estMembreActif(membre) || membre.getStatut() == Statut.DOCTORANT) return false;
        return estSupervision(membre) || membre.getRole() == Role.CHEF_EQUIPE;
    }

    public void verifierPlanificationReunion(Membre membre) {
        verifierAccesReunions(membre);
        if (!peutPlanifierReunion(membre)) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Seuls la direction, les chefs d'équipe et les administrateurs peuvent planifier une réunion.");
        }
    }

    public boolean estSupervisionPourReunions(Membre membre) {
        return estMembreActif(membre) && estSupervision(membre);
    }

    public boolean peutVoirToutesReunions(Membre membre) {
        return estSupervisionPourReunions(membre);
    }

    public boolean peutConsulterReunion(Membre membre, Reunion reunion) {
        return peutVoirToutesReunions(membre)
                || (estMembreActif(membre) && reunion != null
                    && (estOrganisateurReunion(membre, reunion) || estParticipantReunion(membre, reunion)));
    }

    public void verifierConsultationReunion(Membre membre, Reunion reunion) {
        verifierAccesReunions(membre);
        if (!peutConsulterReunion(membre, reunion)) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Cette réunion est réservée aux membres invités.");
        }
    }

    public void verifierGestionReunion(Membre membre, Reunion reunion) {
        verifierAccesReunions(membre);
        if (!estSupervision(membre) && !estOrganisateurReunion(membre, reunion)) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Seul l'organisateur ou la direction peut gérer cette réunion.");
        }
    }

    public boolean peutGererReunion(Membre membre, Reunion reunion) {
        return estMembreActif(membre) && reunion != null
                && (estSupervision(membre) || estOrganisateurReunion(membre, reunion));
    }

    public boolean peutParticiperReunion(Membre membre) {
        return estMembreActif(membre)
                && (membre.getStatut() == Statut.PERMANENT || membre.getStatut() == Statut.ASSOCIE);
    }

    private boolean estOrganisateurReunion(Membre membre, Reunion reunion) {
        return membre != null && reunion.getOrganisateur() != null
                && Objects.equals(membre.getId(), reunion.getOrganisateur().getId());
    }

    private boolean estParticipantReunion(Membre membre, Reunion reunion) {
        return membre != null && reunion.getParticipants() != null
                && reunion.getParticipants().stream()
                        .anyMatch(participant -> Objects.equals(membre.getId(), participant.getId()));
    }

    public void verifierDepotDocument(Membre membre) {
        verifierAccesDocuments(membre);
        if (!estMembrePermanent(membre) && !estSupervision(membre)) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Seuls les membres permanents actifs peuvent déposer un document.");
        }
    }

    public void verifierGestionDocument(Membre membre, Document document) {
        verifierAccesDocuments(membre);
        boolean deposant = document.getDeposePar() != null
                && Objects.equals(document.getDeposePar().getId(), membre.getId());
        if (!deposant && !estSupervision(membre)) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Seul le déposant ou un administrateur, directeur ou vice-directeur peut gérer ce document.");
        }
    }

    public boolean peutGererDocument(Membre membre, Document document) {
        return membre != null
                && membre.isCompteActif()
                && !membre.isCompteGele()
                && membre.getStatut() != Statut.DOCTORANT
                && membre.getStatut() != Statut.RETRAITE
                && membre.getStatut() != Statut.ANCIEN
                && (estSupervision(membre) || (document.getDeposePar() != null
                        && Objects.equals(document.getDeposePar().getId(), membre.getId())));
    }

    public void verifierCreationEvenement(Membre membre) {
        verifierMembreActif(membre);
        if (membre.getStatut() != Statut.PERMANENT && !estAdministrateurOuDirecteur(membre)) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Seuls les membres permanents actifs et les administrateurs ou directeurs peuvent créer un événement.");
        }
    }

    public void verifierGestionEvenement(Evenement evenement, Membre membre) {
        verifierMembreActif(membre);
        boolean organisateurPermanent = estMembrePermanent(membre)
                && evenement.getOrganisateurs() != null
                && evenement.getOrganisateurs().stream()
                        .anyMatch(organisateur -> Objects.equals(organisateur.getId(), membre.getId()));
        if (!estSupervision(membre) && !organisateurPermanent) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Seul un organisateur permanent de cet événement ou un administrateur, directeur ou vice-directeur peut le gérer.");
        }
    }

    public void verifierGestionOrganisateursAnnules(Membre membre) {
        verifierMembreActif(membre);
    }

    public void verifierInscriptionEvenement(Membre membre) {
        verifierMembreActif(membre);
        if (membre.getStatut() != Statut.PERMANENT && membre.getStatut() != Statut.ASSOCIE) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Seuls les membres permanents et associés peuvent s'inscrire aux événements.");
        }
    }

    public boolean peutGererEvenement(Membre membre, Evenement evenement) {
        return estMembreActif(membre)
                && (estSupervision(membre)
                || (estMembrePermanent(membre)
                && evenement.getOrganisateurs() != null
                && evenement.getOrganisateurs().stream()
                        .anyMatch(organisateur -> Objects.equals(organisateur.getId(), membre.getId()))));
    }

    public boolean peutAnnulerEvenement(Membre membre, Evenement evenement) {
        return peutGererEvenement(membre, evenement);
    }

    public boolean estSupervisionPourEvenements(Membre membre) {
        return estMembreActif(membre) && estSupervision(membre);
    }

    public boolean estOrganisateur(Membre membre, Evenement evenement) {
        return membre != null && evenement.getOrganisateurs() != null
                && evenement.getOrganisateurs().stream()
                        .anyMatch(organisateur -> Objects.equals(organisateur.getId(), membre.getId()));
    }

    public void verifierMembreOrganisateurEligible(Membre membre) {
        if (!estMembreActif(membre)
                || membre.getStatut() == Statut.DOCTORANT
                || membre.getStatut() == Statut.RETRAITE
                || membre.getStatut() == Statut.ANCIEN) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Un organisateur doit être un membre actif non doctorant.");
        }
    }

    public boolean peutArchiverPublication(Membre membre, Publication publication) {
        return peutArchiverPublication(membre, publication, null);
    }

    private boolean peutArchiverPublication(Membre membre, Publication publication, Authentication authentication) {
        boolean owner = publication.getAuteurs() != null
                && publication.getAuteurs().stream()
                        .anyMatch(auteur -> Objects.equals(auteur.getId(), membre.getId()));
        return estSuperviseur(membre, authentication) || owner;
    }

    private void verifierMembreActif(Membre membre) {
        if (!estMembreActif(membre)) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Votre compte est inactif, gelé ou ne donne plus accès à l'application.");
        }
    }

    private boolean estMembreActif(Membre membre) {
        return membre != null && membre.isCompteActif() && !membre.isCompteGele()
                && membre.getStatut() != Statut.RETRAITE
                && membre.getStatut() != Statut.ANCIEN;
    }

    private boolean estMembrePermanent(Membre membre) {
        return estMembreActif(membre) && membre.getStatut() == Statut.PERMANENT;
    }

    private boolean estSupervision(Membre membre) {
        return estSuperviseur(membre, null);
    }

    private boolean estSuperviseur(Membre membre, Authentication authentication) {
        return membre != null && (membre.getRole() == Role.ADMIN
                || membre.getRole() == Role.DIRECTEUR
                || membre.getRole() == Role.VICE_DIRECTEUR
                || membre.getRoleSysteme() == RoleUtilisateur.ADMIN
                || (authentication != null && authentication.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ADMIN")
                                || authority.getAuthority().equals("DIRECTEUR")
                                || authority.getAuthority().equals("VICE_DIRECTEUR"))));
    }

    private boolean estAdministrateurOuDirecteur(Membre membre) {
        return membre != null && (membre.getRole() == Role.ADMIN
                || membre.getRole() == Role.DIRECTEUR
                || membre.getRoleSysteme() == RoleUtilisateur.ADMIN
        );
    }

    // =========================================================================
    // MESSAGERIE INTERNE ET NOTIFICATIONS (SECTIONS 13 ET 19)
    // =========================================================================

    public void verifierAccesMessagerieInterne(Membre membre) {
        verifierMembreActif(membre);
        if (membre.getStatut() == Statut.DOCTORANT || membre.getRoleSysteme() == RoleUtilisateur.DOCTORANT) {
            throw new ResponseStatusException(FORBIDDEN,
                    "La messagerie interne n'est pas accessible aux doctorants (section 2.2.C).");
        }
    }

    public boolean peutEnvoyerMessageEquipe(Membre membre, com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire equipe) {
        if (!estMembreActif(membre)) return false;
        if (membre.getStatut() != Statut.PERMANENT && !estSupervision(membre)) return false;
        return membre.getEquipeLaboratoire() == equipe || estSupervision(membre);
    }

    public void verifierEnvoiMessageEquipe(Membre membre, com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire equipe) {
        verifierAccesMessagerieInterne(membre);
        if (membre.getStatut() == Statut.ASSOCIE) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Les membres associés ne peuvent pas participer aux canaux d'équipe.");
        }
        if (!peutEnvoyerMessageEquipe(membre, equipe)) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Vous n'avez pas l'autorisation d'envoyer un message à l'équipe " + equipe);
        }
    }

    public boolean peutAccederDiscussionEvenement(Membre membre, Evenement evenement) {
        if (!estMembreActif(membre)) return false;
        if (membre.getStatut() == Statut.DOCTORANT || membre.getRoleSysteme() == RoleUtilisateur.DOCTORANT) {
            return false;
        }
        if (estSupervision(membre)) return true;
        boolean estOrganisateur = evenement.getOrganisateurs() != null &&
                evenement.getOrganisateurs().stream().anyMatch(o -> Objects.equals(o.getId(), membre.getId()));
        boolean estParticipant = evenement.getParticipants() != null &&
                evenement.getParticipants().stream().anyMatch(p -> Objects.equals(p.getId(), membre.getId()));
        return estOrganisateur || estParticipant;
    }

    public void verifierAccesDiscussionEvenement(Membre membre, Evenement evenement) {
        verifierAccesMessagerieInterne(membre);
        if (!peutAccederDiscussionEvenement(membre, evenement)) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Vous devez être organisateur ou participant pour accéder à la discussion de cet événement.");
        }
    }
}

