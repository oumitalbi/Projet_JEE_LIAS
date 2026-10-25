package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import com.lias.gestion_laboratoire.dto.HistoriqueModificationProfilResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;

/**
 * Service métier pour la gestion des membres du laboratoire de recherche LIAS.
 *
 * <p>Cette classe centralise toute la logique métier relative aux membres :
 * ajout avec contrôle d'unicité, consultation filtrée, recherche globale
 * et désactivation logique (soft delete).</p>
 *
 * <p><strong>Règle fondamentale :</strong> Aucun membre n'est jamais supprimé
 * physiquement de la base de données. Les changements de statut (retraite,
 * départ) désactivent ou gèlent le compte tout en conservant l'historique
 * complet en base de données.</p>
 *
 * <p><strong>Traçabilité :</strong> Chaque changement de statut est enregistré
 * dans l'historique des affiliations via {@link HistoriqueAffiliationService}.</p>
 *
 * @author LIAS Laboratory
 * @see Membre
 * @see MembreRepository
 * @see HistoriqueAffiliationService
 */
@Service
@Transactional
@RequiredArgsConstructor
public class MembreService {

    /** Repository injecté via le constructeur généré par Lombok. */
    private final MembreRepository membreRepository;

    /** Service d'historique des affiliations pour la traçabilité. */
    private final HistoriqueAffiliationService historiqueAffiliationService;
    private final HistoriqueModificationProfilService historiqueModificationProfilService;
    private final MandatService mandatService;

    // =========================================================================
    // CRÉATION
    // =========================================================================

    /**
     * Ajoute un nouveau membre au laboratoire LIAS après vérification
     * de l'unicité de son adresse e-mail.
     *
     * <p><strong>Règle métier :</strong> Deux membres (actifs ou désactivés)
     * ne peuvent pas partager la même adresse e-mail. Si l'email fourni
     * existe déjà en base, une exception est levée avant toute persistance.</p>
     *
     * <p>Une première ligne d'historique d'affiliation est automatiquement
     * créée pour assurer la traçabilité dès l'inscription.</p>
     *
     * @param membre l'entité membre à persister
     * @return le membre sauvegardé avec son identifiant généré
     * @throws IllegalArgumentException si l'adresse e-mail est déjà utilisée
     */
    public Membre ajouterMembre(Membre membre) {
        if (membreRepository.existsByEmail(membre.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }
        Membre membreSauvegarde = membreRepository.save(membre);

        // Créer la première ligne d'historique d'affiliation
        historiqueAffiliationService.creerPremiereAffiliation(membreSauvegarde);

        return membreSauvegarde;
    }

    // =========================================================================
    // CONSULTATION — MEMBRES ACTIFS
    // =========================================================================

    /**
     * Récupère la liste de tous les membres dont le compte est actif.
     *
     * <p>Cette méthode constitue le point d'entrée principal pour l'affichage
     * des membres dans l'interface de gestion. Elle exclut automatiquement
     * les membres désactivés (soft-deleted).</p>
     *
     * @return liste de tous les membres actifs du laboratoire
     */
    @Transactional(readOnly = true)
    public List<Membre> obtenirTousLesMembresActifs() {
        return membreRepository.findByCompteActif(true);
    }

    // =========================================================================
    // FILTRAGE PAR ÉQUIPE
    // =========================================================================

    /**
     * Récupère les membres actifs rattachés à une équipe de recherche
     * spécifique du laboratoire LIAS.
     *
     * <p><strong>Cas d'usage :</strong> afficher la composition d'une équipe
     * (ISDIAC, SIMA, ILIAS, SDTIC) sur le tableau de bord ou dans les
     * rapports d'évaluation.</p>
     *
     * @param equipe l'équipe de recherche ciblée
     * @return liste des membres actifs appartenant à l'équipe spécifiée
     */
    @Transactional(readOnly = true)
    public List<Membre> obtenirMembresParEquipe(EquipeLaboratoire equipe) {
        return membreRepository.findByEquipeLaboratoireAndCompteActif(equipe, true);
    }

    // =========================================================================
    // RECHERCHE GLOBALE
    // =========================================================================

    /**
     * Recherche des membres actifs par nom ou prénom (recherche partielle,
     * insensible à la casse).
     *
     * <p>Le même mot-clé est utilisé pour interroger à la fois le champ
     * {@code nom} et le champ {@code prenom}. Seuls les membres dont le
     * compte est actif sont retournés.</p>
     *
     * <p><strong>Cas d'usage :</strong> barre de recherche dans l'interface
     * de gestion des membres du laboratoire LIAS.</p>
     *
     * @param motCle le fragment de texte à rechercher dans le nom ou le prénom
     * @return liste des membres actifs correspondant au mot-clé
     */
    @Transactional(readOnly = true)
    public List<Membre> rechercherMembres(String motCle) {
        return membreRepository
                .findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCaseAndCompteActif(
                        motCle, motCle, true);
    }

    // =========================================================================
    // SOFT DELETE — MISE EN RETRAITE
    // =========================================================================

    /**
     * Met un membre en retraite (compte gelé, accès bloqué).
     *
     * <p><strong>Règle métier :</strong> Le membre passe au statut
     * {@link Statut#RETRAITE}, son compte est gelé ({@code compteGele = true})
     * et désactivé ({@code compteActif = false}). Aucune donnée n'est
     * supprimée de la base de données.</p>
     *
     * <p>Le changement de statut est enregistré dans l'historique
     * des affiliations pour assurer la traçabilité.</p>
     *
     * @param id l'identifiant du membre à mettre en retraite
     * @return le membre mis à jour
     * @throws EntityNotFoundException si aucun membre n'est trouvé pour l'ID fourni
     */
    public Membre mettreEnRetraite(Long id) {
        Membre membre = membreRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucun membre trouvé avec l'ID : " + id));

        mandatService.cloturerMandatsMembre(membre, "Mise à la retraite du titulaire.");
        historiqueAffiliationService.enregistrerChangementStatut(
                membre, Statut.RETRAITE,
                "Départ en retraite — compte gelé");

        membre.setStatut(Statut.RETRAITE);
        membre.setCompteGele(true);
        membre.setCompteActif(false);

        historiqueModificationProfilService.enregistrer(membre, "RETRAITE", "Statut : " + Statut.RETRAITE);
        return membreRepository.save(membre);
    }

    // =========================================================================
    // SOFT DELETE — DÉSACTIVATION COMME ANCIEN MEMBRE
    // =========================================================================

    /**
     * Désactive un membre en le marquant comme ancien membre.
     *
     * <p><strong>Règle métier :</strong> Le membre passe au statut
     * {@link Statut#ANCIEN}, son compte est désactivé
     * ({@code compteActif = false}). Aucune donnée n'est supprimée
     * de la base de données.</p>
     *
     * <p>Le changement de statut est enregistré dans l'historique
     * des affiliations pour assurer la traçabilité.</p>
     *
     * @param id l'identifiant du membre à désactiver
     * @return le membre mis à jour
     * @throws EntityNotFoundException si aucun membre n'est trouvé pour l'ID fourni
     */
    public Membre desactiverCommeAncienMembre(Long id) {
        Membre membre = membreRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucun membre trouvé avec l'ID : " + id));

        mandatService.cloturerMandatsMembre(membre, "Départ du laboratoire.");
        historiqueAffiliationService.enregistrerChangementStatut(
                membre, Statut.ANCIEN,
                "Départ du laboratoire — compte désactivé");

        membre.setStatut(Statut.ANCIEN);
        membre.setCompteActif(false);

        historiqueModificationProfilService.enregistrer(membre, "DESACTIVATION", "Statut : " + Statut.ANCIEN);
        return membreRepository.save(membre);
    }

    // =========================================================================
    // RÉACTIVATION D'UN ANCIEN MEMBRE (RETOUR AU LABO)
    // =========================================================================

    /**
     * Réactive un ancien membre ou un retraité qui revient au laboratoire.
     *
     * <p><strong>Règle métier :</strong> Le membre repasse au statut spécifié
     * (typiquement {@link Statut#PERMANENT} ou {@link Statut#ASSOCIE}),
     * son compte est réactivé et dégelé. Une nouvelle période d'affiliation
     * est ouverte dans l'historique.</p>
     *
     * @param id           l'identifiant du membre à réactiver
     * @param nouveauStatut le statut de retour du membre
     * @return le membre réactivé
     * @throws EntityNotFoundException si aucun membre n'est trouvé pour l'ID fourni
     */
    public Membre reactiverMembre(Long id, Statut nouveauStatut) {
        if (nouveauStatut != Statut.PERMANENT && nouveauStatut != Statut.ASSOCIE) {
            throw new IllegalArgumentException("Un membre réactivé doit être permanent ou associé.");
        }
        Membre membre = membreRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucun membre trouvé avec l'ID : " + id));
        if (membre.isCompteActif() && membre.getStatut() != Statut.RETRAITE
                && membre.getStatut() != Statut.ANCIEN) {
            throw new IllegalArgumentException("Seul un ancien membre ou un retraité peut être réactivé.");
        }

        // Enregistrer le changement dans l'historique
        historiqueAffiliationService.enregistrerChangementStatut(
                membre, nouveauStatut,
                "Retour au laboratoire — compte réactivé");

        Statut ancienStatut = membre.getStatut();
        membre.setStatut(nouveauStatut);
        membre.setCompteActif(true);
        membre.setCompteGele(false);

        historiqueModificationProfilService.enregistrer(membre, "REACTIVATION",
                "Statut : " + ancienStatut + " → " + nouveauStatut);
        return membreRepository.save(membre);
    }
    // =========================================================================
    // MODIFICATION DE PROFIL PERSONNEL
    // =========================================================================

    /**
     * Met à jour les informations personnelles d'un membre.
     *
     * <p><strong>Règle métier (sections 3 et 21) :</strong> Un membre ne peut modifier
     * que ses propres informations. Les champs liés à la gouvernance (rôle, statut, 
     * équipe, état du compte, email) ne doivent jamais être modifiés via cette méthode.</p>
     *
     * @param membreId l'identifiant du membre à mettre à jour
     * @param request  les nouvelles informations personnelles
     * @return le membre mis à jour
     * @throws EntityNotFoundException si aucun membre n'est trouvé pour l'ID fourni
     */
    public Membre modifierMonProfil(Long membreId, com.lias.gestion_laboratoire.dto.MembreUpdateProfilRequest request) {
        Membre membre = membreRepository.findById(membreId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucun membre trouvé avec l'ID : " + membreId));

        List<String> changements = new ArrayList<>();
        appliquerChangement(changements, "titreAcademique", membre.getTitreAcademique(),
                request.getTitreAcademique(), membre::setTitreAcademique);
        appliquerChangement(changements, "nom", membre.getNom(), request.getNom(), membre::setNom);
        appliquerChangement(changements, "prenom", membre.getPrenom(), request.getPrenom(), membre::setPrenom);
        appliquerChangement(changements, "photo", membre.getPhoto(), request.getPhoto(), membre::setPhoto);
        appliquerChangement(changements, "biographie", membre.getBiographie(),
                request.getBiographie(), membre::setBiographie);
        appliquerChangement(changements, "etablissementOrigine", membre.getEtablissementOrigine(),
                request.getEtablissementOrigine(), membre::setEtablissementOrigine);

        if (!changements.isEmpty()) {
            historiqueModificationProfilService.enregistrer(membre, "MODIFICATION_PROFIL",
                    String.join("; ", changements));
        }
        return membreRepository.save(membre);
    }

    @Transactional(readOnly = true)
    public List<HistoriqueModificationProfilResponse> obtenirHistoriqueProfil(Long membreId) {
        return historiqueModificationProfilService.obtenirHistorique(membreId);
    }

    private <T> void appliquerChangement(List<String> changements, String champ, T valeurActuelle,
                                         T valeurDemandee, java.util.function.Consumer<T> setter) {
        if (valeurDemandee != null && !java.util.Objects.equals(valeurActuelle, valeurDemandee)) {
            changements.add(champ + " : " + valeurActuelle + " → " + valeurDemandee);
            setter.accept(valeurDemandee);
        }
    }

    // =========================================================================
    // UPLOAD DE PHOTO
    // =========================================================================

    /**
     * Uploade et met à jour la photo de profil du membre.
     *
     * @param membreId l'identifiant du membre
     * @param file     le fichier image uploadé
     * @param uploadDir le dossier d'upload configuré
     * @return le membre avec le chemin de sa nouvelle photo
     */
    public Membre uploaderPhoto(Long membreId, org.springframework.web.multipart.MultipartFile file, String uploadDir) throws java.io.IOException {
        Membre membre = membreRepository.findById(membreId)
                .orElseThrow(() -> new EntityNotFoundException("Aucun membre trouvé avec l'ID : " + membreId));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }

        // Créer le dossier s'il n'existe pas
        java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!java.nio.file.Files.exists(uploadPath)) {
            java.nio.file.Files.createDirectories(uploadPath);
        }

        // Générer un nom de fichier unique
        String originalFilename = org.springframework.util.StringUtils.cleanPath(java.util.Objects.requireNonNull(file.getOriginalFilename()));
        String extension = "";
        int i = originalFilename.lastIndexOf('.');
        if (i > 0) {
            extension = originalFilename.substring(i);
        }
        String newFilename = "profile_" + membreId + "_" + System.currentTimeMillis() + extension;

        // Copier le fichier
        java.nio.file.Path targetLocation = uploadPath.resolve(newFilename);
        java.nio.file.Files.copy(file.getInputStream(), targetLocation, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Enregistrer le chemin relatif dans la BD (ex: http://localhost:8080/uploads/photos/...)
        String fileUrl = "http://localhost:8080/uploads/photos/" + newFilename;
        membre.setPhoto(fileUrl);

        historiqueModificationProfilService.enregistrer(membre, "PHOTO_MODIFIEE", "Photo de profil mise à jour.");
        return membreRepository.save(membre);
    }
}
