package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.models.HistoriqueAffiliation;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.repositories.HistoriqueAffiliationRepository;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service métier pour la gestion de l'historique des affiliations
 * des membres du laboratoire de recherche LIAS.
 *
 * <p>Ce service assure la traçabilité complète du parcours de chaque
 * membre au sein du laboratoire. À chaque changement de statut
 * (départ, retour, retraite, etc.), une nouvelle ligne d'historique
 * est créée et la période précédente est clôturée.</p>
 *
 * @author LIAS Laboratory
 * @see HistoriqueAffiliation
 * @see HistoriqueAffiliationRepository
 */
@Service
@Transactional
@RequiredArgsConstructor
public class HistoriqueAffiliationService {

    private final HistoriqueAffiliationRepository historiqueRepository;
    private final MembreRepository membreRepository;

    // =========================================================================
    // CRÉATION DE LA PREMIÈRE LIGNE D'HISTORIQUE
    // =========================================================================

    /**
     * Crée la première ligne d'historique pour un nouveau membre.
     *
     * <p>Appelée lors de l'ajout d'un nouveau membre au laboratoire.
     * La date de début est la date d'affiliation du membre ou la date
     * du jour si non renseignée. La date de fin est {@code null}
     * (période en cours).</p>
     *
     * @param membre le membre nouvellement créé
     */
    public void creerPremiereAffiliation(Membre membre) {
        HistoriqueAffiliation historique = new HistoriqueAffiliation();
        historique.setMembre(membre);
        historique.setDateDebut(
                membre.getDateAffiliation() != null
                        ? membre.getDateAffiliation()
                        : LocalDate.now());
        historique.setStatut(membre.getStatut());
        historique.setMotif("Première affiliation au laboratoire LIAS");

        historiqueRepository.save(historique);
    }

    // =========================================================================
    // ENREGISTREMENT D'UN CHANGEMENT DE STATUT
    // =========================================================================

    /**
     * Enregistre un changement de statut pour un membre du laboratoire.
     *
     * <p>Cette méthode :
     * <ol>
     *   <li>Clôture la période d'affiliation en cours (si elle existe)
     *       en fixant sa {@code dateFin} à aujourd'hui</li>
     *   <li>Crée une nouvelle période d'affiliation avec le nouveau
     *       statut et {@code dateFin = null}</li>
     * </ol>
     * </p>
     *
     * @param membre        le membre concerné
     * @param nouveauStatut le nouveau statut du membre
     * @param motif         le motif du changement de statut
     */
    public void enregistrerChangementStatut(Membre membre,
                                            Statut nouveauStatut,
                                            String motif) {
        if (membre.getStatut() == nouveauStatut) {
            return;
        }
        // Clôturer la période en cours (si elle existe)
        Optional<HistoriqueAffiliation> periodeEnCours =
                historiqueRepository.findByMembre_IdAndDateFinIsNull(membre.getId());
        periodeEnCours.ifPresent(h -> {
            h.setDateFin(LocalDate.now());
            historiqueRepository.save(h);
        });

        if (nouveauStatut != Statut.RETRAITE && nouveauStatut != Statut.ANCIEN) {
            HistoriqueAffiliation nouvelleperiode = new HistoriqueAffiliation();
            nouvelleperiode.setMembre(membre);
            nouvelleperiode.setDateDebut(LocalDate.now());
            nouvelleperiode.setStatut(nouveauStatut);
            nouvelleperiode.setMotif(motif);
            historiqueRepository.save(nouvelleperiode);
        }
    }

    // =========================================================================
    // CONSULTATION DE L'HISTORIQUE
    // =========================================================================

    /**
     * Récupère l'historique complet des affiliations d'un membre,
     * trié de la plus récente à la plus ancienne.
     *
     * @param membreId l'identifiant du membre
     * @return liste de toutes les périodes d'affiliation du membre
     */
    @Transactional(readOnly = true)
    public List<HistoriqueAffiliation> obtenirHistoriqueParMembre(Long membreId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Membre auteur) || !auteur.isCompteActif() || auteur.isCompteGele()
                || auteur.getStatut() == Statut.DOCTORANT || auteur.getStatut() == Statut.RETRAITE
                || auteur.getStatut() == Statut.ANCIEN) {
            throw new AccessDeniedException("Un membre actif doit être connecté.");
        }
        if (!membreRepository.existsById(membreId)) {
            throw new jakarta.persistence.EntityNotFoundException("Membre introuvable : " + membreId);
        }
        return historiqueRepository.findByMembre_IdOrderByDateDebutDesc(membreId);
    }
}
