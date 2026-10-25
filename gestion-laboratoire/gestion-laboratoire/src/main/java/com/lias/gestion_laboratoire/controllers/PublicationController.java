package com.lias.gestion_laboratoire.controllers;

import com.lias.gestion_laboratoire.models.Publication;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.services.PublicationService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Contrôleur REST exposant les endpoints de gestion des publications
 * scientifiques du laboratoire de recherche LIAS.
 *
 * <p>
 * Base URL : {@code /api/publications}
 * </p>
 *
 * <p>
 * Ce contrôleur délègue l'intégralité de la logique métier au
 * {@link PublicationService} et se concentre uniquement sur la gestion
 * des requêtes HTTP et des codes de statut de réponse.
 * </p>
 *
 * <p>
 * <strong>Convention soft delete :</strong> L'endpoint {@code DELETE /{id}}
 * ne supprime jamais physiquement une publication ; il l'archive
 * ({@code estArchive = true}).
 * </p>
 *
 * @author LIAS Laboratory
 * @see PublicationService
 */
@RestController
@RequestMapping("/api/publications")
@RequiredArgsConstructor
public class PublicationController {

    /** Service métier injecté via le constructeur généré par Lombok. */
    private final PublicationService publicationService;

    // =========================================================================
    // DTO INTERNE — REQUÊTE DE CRÉATION
    // =========================================================================

    /**
     * DTO (Data Transfer Object) interne utilisé pour la création d'une
     * publication via le endpoint {@code POST /api/publications}.
     *
     * <p>
     * Encapsule l'entité {@link Publication} à persister ainsi que
     * l'ensemble des identifiants des membres co-auteurs.
     * </p>
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicationCreateRequest {

        /** La publication à créer. */
        private Publication publication;

        /** Ensemble des identifiants des membres co-auteurs. */
        private Set<Long> auteursIds;
    }

    // =========================================================================
    // GET /api/publications
    // =========================================================================

    /**
     * Récupère la liste complète des publications non archivées du laboratoire.
     *
     * <p>
     * <strong>Exemple :</strong> {@code GET /api/publications}
     * </p>
     *
     * @return {@code 200 OK} avec la liste des publications actives
     */
    @GetMapping
    public ResponseEntity<List<Publication>> obtenirToutesLesPublications() {
        List<Publication> publications = publicationService.obtenirToutesLesPublications();
        return ResponseEntity.ok(publications);
    }

    // =========================================================================
    // GET /api/publications/auteur/{membreId}
    // =========================================================================

    /**
     * Récupère les publications non archivées d'un auteur spécifique.
     *
     * <p>
     * <strong>Exemple :</strong> {@code GET /api/publications/auteur/5}
     * </p>
     *
     * @param membreId l'identifiant du membre auteur (path variable)
     * @return {@code 200 OK} avec la liste des publications de l'auteur
     */
    @GetMapping("/auteur/{membreId}")
    public ResponseEntity<List<Publication>> obtenirPublicationsParAuteur(
            @PathVariable("membreId") Long membreId) {
        List<Publication> publications = publicationService.obtenirPublicationsParAuteur(membreId);
        return ResponseEntity.ok(publications);
    }

    // =========================================================================
    // GET /api/publications/equipe/{equipe}
    // =========================================================================

    /**
     * Récupère les publications non archivées d'une équipe de recherche.
     *
     * <p>
     * La variable de chemin {@code {equipe}} est automatiquement convertie
     * en valeur de l'enum {@link EquipeLaboratoire} par Spring.
     * Valeurs acceptées : {@code ISDIAC}, {@code SIMA}, {@code ILIAS},
     * {@code SDTIC}, {@code AUCUNE}.
     * </p>
     *
     * <p>
     * <strong>Exemple :</strong> {@code GET /api/publications/equipe/ILIAS}
     * </p>
     *
     * @param equipe l'acronyme de l'équipe de recherche (path variable)
     * @return {@code 200 OK} avec la liste des publications de l'équipe
     */
    @GetMapping("/equipe/{equipe}")
    public ResponseEntity<List<Publication>> obtenirPublicationsParEquipe(
            @PathVariable("equipe") EquipeLaboratoire equipe) {
        List<Publication> publications = publicationService.obtenirPublicationsParEquipe(equipe);
        return ResponseEntity.ok(publications);
    }

    // =========================================================================
    // POST /api/publications
    // =========================================================================

    /**
     * Crée et persiste une nouvelle publication dans le laboratoire LIAS.
     *
     * <p>Le corps de la requête doit contenir un objet JSON avec deux champs :
     * {@code publication} (l'entité à créer) et {@code auteursIds} (ensemble
     * des identifiants des membres co-auteurs).</p>
     *
     * <p><strong>Exemple :</strong> {@code POST /api/publications} avec le
     * corps JSON :</p>
     * <pre>{@code
     * {
     * "publication": { "titre": "...", "type": "ARTICLE_JOURNAL", ... },
     * "auteursIds": [1, 3, 7]
     * }
     * }</pre>
     *
     * @param request le DTO contenant la publication et les IDs des auteurs
     * @return {@code 201 Created} avec la publication persistée,
     * ou {@code 404 Not Found} si un auteur n'existe pas
     */
    @PostMapping
    public ResponseEntity<Publication> ajouterPublication(
            @RequestBody PublicationCreateRequest request) {
        Publication publication = publicationService.ajouterPublication(
                request.getPublication(), request.getAuteursIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(publication);
    }

    // =========================================================================
    // DELETE /api/publications/{id}
    // =========================================================================

    /**
     * Archive logiquement une publication du laboratoire (soft delete).
     *
     * <p>
     * <strong>Règle métier :</strong> Cet endpoint ne supprime <em>jamais</em>
     * physiquement un enregistrement. Il passe le champ {@code estArchive}
     * à {@code true}, retirant la publication de l'affichage actif.
     * </p>
     *
     * <p>
     * <strong>Exemple :</strong> {@code DELETE /api/publications/12}
     * </p>
     *
     * @param id l'identifiant de la publication à archiver (path variable)
     * @return {@code 204 No Content} si l'archivage a réussi,
     *         ou {@code 404 Not Found} si aucune publication n'existe pour cet ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiverPublication(@PathVariable("id") Long id) {
        publicationService.archiverPublication(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère la liste des publications archivées.
     */
    @GetMapping("/archivees")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('DIRECTEUR')")
    public ResponseEntity<List<Publication>> obtenirPublicationsArchivees() {
        List<Publication> publications = publicationService.obtenirPublicationsArchivees();
        return ResponseEntity.ok(publications);
    }

    /**
     * Désarchive une publication.
     */
    @PutMapping("/{id}/desarchiver")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('DIRECTEUR')")
    public ResponseEntity<Publication> desarchiverPublication(@PathVariable("id") Long id) {
        Publication publication = publicationService.desarchiverPublication(id);
        return ResponseEntity.ok(publication);
    }
}
