package com.lias.gestion_laboratoire.controllers;

import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.services.MembreService;
import com.lias.gestion_laboratoire.services.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import com.lias.gestion_laboratoire.dto.MembreUpdateProfilRequest;

/**
 * Contrôleur REST exposant les endpoints de gestion des membres
 * du laboratoire de recherche LIAS.
 *
 * <p>Base URL : {@code /api/membres}</p>
 *
 * <p>Ce contrôleur délègue l'intégralité de la logique métier au
 * {@link MembreService} et se concentre uniquement sur la gestion
 * des requêtes HTTP et des codes de statut de réponse.</p>
 *
 * <p><strong>Convention soft delete :</strong> Aucun endpoint ne supprime
 * physiquement un membre. Les changements de statut se font exclusivement
 * via des endpoints {@code @PutMapping} dédiés (retraite, désactivation,
 * réactivation).</p>
 *
 * @author LIAS Laboratory
 * @see MembreService
 */
@RestController
@RequestMapping("/api/membres")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class MembreController {

    /** Service métier injecté via le constructeur généré par Lombok. */
    private final MembreService membreService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/{id}/reinitialiser-mot-de-passe")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, String>> envoyerLienReinitialisation(@PathVariable Long id) {
        passwordResetService.envoyerLienParAdministration(id);
        return ResponseEntity.accepted().body(Map.of(
                "message", "Un lien temporaire a été envoyé à l’adresse enregistrée du membre."));
    }

    // =========================================================================
    // GET /api/membres
    // =========================================================================

    /**
     * Récupère la liste complète des membres actifs du laboratoire LIAS.
     *
     * <p>Seuls les membres dont {@code compteActif = true} sont retournés,
     * conformément à la règle de non-suppression (soft delete).</p>
     *
     * @return {@code 200 OK} avec la liste des membres actifs en corps de réponse
     */
    @GetMapping
    public ResponseEntity<List<Membre>> obtenirTousLesMembresActifs() {
        List<Membre> membres = membreService.obtenirTousLesMembresActifs();
        return ResponseEntity.ok(membres);
    }

    @GetMapping("/{id}/historique-profil")
    public ResponseEntity<List<com.lias.gestion_laboratoire.dto.HistoriqueModificationProfilResponse>>
            obtenirHistoriqueProfil(@PathVariable Long id) {
        return ResponseEntity.ok(membreService.obtenirHistoriqueProfil(id));
    }

    // =========================================================================
    // GET /api/membres/equipe/{equipe}
    // =========================================================================

    /**
     * Récupère les membres actifs appartenant à une équipe de recherche
     * spécifique du laboratoire LIAS.
     *
     * <p>La variable de chemin {@code {equipe}} est automatiquement convertie
     * en valeur de l'enum {@link EquipeLaboratoire} par Spring.
     * Valeurs acceptées : {@code ISDIAC}, {@code SIMA}, {@code ILIAS},
     * {@code SDTIC}, {@code AUCUNE}.</p>
     *
     * <p><strong>Exemple :</strong> {@code GET /api/membres/equipe/ILIAS}</p>
     *
     * @param equipe l'acronyme de l'équipe de recherche (path variable)
     * @return {@code 200 OK} avec la liste des membres actifs de l'équipe,
     *         ou {@code 400 Bad Request} si la valeur d'équipe est invalide
     */
    @GetMapping("/equipe/{equipe}")
    public ResponseEntity<List<Membre>> obtenirMembresParEquipe(
            @PathVariable("equipe") EquipeLaboratoire equipe) {
        List<Membre> membres = membreService.obtenirMembresParEquipe(equipe);
        return ResponseEntity.ok(membres);
    }

    // =========================================================================
    // GET /api/membres/recherche?motCle=...
    // =========================================================================

    /**
     * Recherche des membres actifs par nom ou prénom (recherche partielle,
     * insensible à la casse).
     *
     * <p>Le paramètre {@code motCle} est utilisé pour interroger simultanément
     * les champs {@code nom} et {@code prenom} de tous les membres actifs.</p>
     *
     * <p><strong>Exemple :</strong> {@code GET /api/membres/recherche?motCle=ali}</p>
     *
     * @param motCle le fragment de texte à rechercher (request param obligatoire)
     * @return {@code 200 OK} avec la liste des membres correspondant au mot-clé
     */
    @GetMapping("/recherche")
    public ResponseEntity<List<Membre>> rechercherMembres(
            @RequestParam("motCle") String motCle) {
        List<Membre> membres = membreService.rechercherMembres(motCle);
        return ResponseEntity.ok(membres);
    }

    // =========================================================================
    // POST /api/membres
    // =========================================================================

    /**
     * Crée et persiste un nouveau membre dans le laboratoire LIAS.
     *
     * <p>Avant la persistance, l'unicité de l'adresse e-mail est vérifiée.
     * Si l'email est déjà utilisé (par un membre actif ou désactivé),
     * la requête est rejetée avec un statut {@code 400 Bad Request}.</p>
     *
     * <p><strong>Exemple :</strong> {@code POST /api/membres}
     * avec un corps JSON représentant l'entité {@link Membre}.</p>
     *
     * @param membre le membre à créer, désérialisé depuis le corps de la requête
     * @return {@code 201 Created} avec le membre persisté en corps de réponse,
     *         ou {@code 400 Bad Request} si l'email est déjà utilisé
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('DIRECTEUR')")
    public ResponseEntity<Membre> ajouterMembre(@RequestBody Membre membre) {
        Membre nouveauMembre = membreService.ajouterMembre(membre);
        return ResponseEntity.status(HttpStatus.CREATED).body(nouveauMembre);
    }

    // =========================================================================
    // PUT /api/membres/{id}/retraite
    // =========================================================================

    /**
     * Met un membre en retraite (soft delete — compte gelé).
     *
     * <p><strong>Règle métier :</strong> Le statut passe à {@code RETRAITE},
     * le compte est gelé et désactivé. Aucune donnée n'est supprimée.
     * Le changement est tracé dans l'historique des affiliations.</p>
     *
     * <p><strong>Exemple :</strong> {@code PUT /api/membres/42/retraite}</p>
     *
     * @param id l'identifiant du membre (path variable)
     * @return {@code 200 OK} avec le membre mis à jour
     */
    @PutMapping("/{id}/retraite")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('DIRECTEUR')")
    public ResponseEntity<Membre> mettreEnRetraite(@PathVariable("id") Long id) {
        Membre membre = membreService.mettreEnRetraite(id);
        return ResponseEntity.ok(membre);
    }

    // =========================================================================
    // PUT /api/membres/{id}/desactiver
    // =========================================================================

    /**
     * Désactive un membre en le marquant comme ancien membre (soft delete).
     *
     * <p><strong>Règle métier :</strong> Le statut passe à {@code ANCIEN},
     * le compte est désactivé. Aucune donnée n'est supprimée.
     * Le changement est tracé dans l'historique des affiliations.</p>
     *
     * <p><strong>Exemple :</strong> {@code PUT /api/membres/42/desactiver}</p>
     *
     * @param id l'identifiant du membre (path variable)
     * @return {@code 200 OK} avec le membre mis à jour
     */
    @PutMapping("/{id}/desactiver")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('DIRECTEUR')")
    public ResponseEntity<Membre> desactiverMembre(@PathVariable("id") Long id) {
        Membre membre = membreService.desactiverCommeAncienMembre(id);
        return ResponseEntity.ok(membre);
    }

    // =========================================================================
    // PUT /api/membres/{id}/reactiver
    // =========================================================================

    /**
     * Réactive un ancien membre ou un retraité qui revient au laboratoire.
     *
     * <p><strong>Règle métier :</strong> Le compte est réactivé et dégelé.
     * Le nouveau statut est spécifié dans le corps de la requête.
     * Le changement est tracé dans l'historique des affiliations.</p>
     *
     * <p><strong>Corps attendu (JSON) :</strong>
     * {@code { "statut": "PERMANENT" }}</p>
     *
     * <p><strong>Exemple :</strong> {@code PUT /api/membres/42/reactiver}</p>
     *
     * @param id      l'identifiant du membre (path variable)
     * @param payload le corps contenant le nouveau statut
     * @return {@code 200 OK} avec le membre réactivé
     */
    @PutMapping("/{id}/reactiver")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('DIRECTEUR')")
    public ResponseEntity<Membre> reactiverMembre(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> payload) {
        Statut nouveauStatut = Statut.valueOf(payload.get("statut"));
        Membre membre = membreService.reactiverMembre(id, nouveauStatut);
        return ResponseEntity.ok(membre);
    }

    // =========================================================================
    // GET /api/membres/moi
    // =========================================================================

    /**
     * Récupère le profil complet du membre actuellement connecté.
     *
     * <p>Pratique pour préremplir le formulaire "Mon Profil" côté frontend.</p>
     *
     * @param authentication l'objet d'authentification injecté par Spring Security
     * @return {@code 200 OK} avec les informations du membre connecté
     */
    @GetMapping("/moi")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Membre> obtenirMonProfil(Authentication authentication) {
        Membre membreConnecte = (Membre) authentication.getPrincipal();
        return ResponseEntity.ok(membreConnecte);
    }

    // =========================================================================
    // PUT /api/membres/moi
    // =========================================================================

    /**
     * Met à jour le profil personnel du membre actuellement connecté.
     *
     * <p><strong>Règle métier (sections 3 et 21) :</strong> Un membre ne peut modifier
     * que ses propres informations. L'ID du membre est extrait du token JWT et non 
     * de l'URL pour garantir la sécurité. Les champs de gouvernance sont ignorés.</p>
     *
     * @param request        les nouvelles informations du profil
     * @param authentication l'objet d'authentification injecté par Spring Security
     * @return {@code 200 OK} avec le membre mis à jour
     */
    @PutMapping("/moi")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Membre> modifierMonProfil(
            @RequestBody MembreUpdateProfilRequest request,
            Authentication authentication) {
        Membre membreConnecte = (Membre) authentication.getPrincipal();
        Membre membreMisAJour = membreService.modifierMonProfil(membreConnecte.getId(), request);
        return ResponseEntity.ok(membreMisAJour);
    }

    // =========================================================================
    // POST /api/membres/moi/photo
    // =========================================================================

    /**
     * Uploade une nouvelle photo de profil pour le membre connecté.
     *
     * @param file           le fichier image uploadé
     * @param authentication l'objet d'authentification injecté par Spring Security
     * @param uploadDir      le dossier d'upload injecté via @Value
     * @return {@code 200 OK} avec le membre mis à jour
     */
    @PostMapping("/moi/photo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploaderMaPhoto(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            Authentication authentication,
            @org.springframework.beans.factory.annotation.Value("${app.upload.dir}") String uploadDir) {
        try {
            Membre membreConnecte = (Membre) authentication.getPrincipal();
            Membre membreMisAJour = membreService.uploaderPhoto(membreConnecte.getId(), file, uploadDir);
            return ResponseEntity.ok(membreMisAJour);
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Collections.singletonMap("error", "Erreur lors de la sauvegarde du fichier"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Collections.singletonMap("error", e.getMessage()));
        }
    }
}
