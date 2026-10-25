package com.lias.gestion_laboratoire.controllers;

import com.lias.gestion_laboratoire.models.Document;
import com.lias.gestion_laboratoire.models.enums.TypeDocument;
import com.lias.gestion_laboratoire.dto.DocumentResponse;
import com.lias.gestion_laboratoire.services.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.nio.charset.StandardCharsets;

/**
 * Contrôleur REST pour la gestion de l'archivage documentaire.
 *
 * <p>Expose les endpoints pour l'upload de fichiers, la consultation des
 * métadonnées et le téléchargement des fichiers physiques rattachés aux
 * événements du laboratoire LIAS.</p>
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Gestion de l'archivage documentaire")
public class DocumentController {

    private final DocumentService documentService;

    // =========================================================================
    // 1. UPLOAD D'UN DOCUMENT
    // =========================================================================

    /**
     * Endpoint pour uploader un document et le rattacher à un événement.
     *
     * @param fichier     le fichier binaire uploadé (multipart)
     * @param evenementId l'identifiant de l'événement cible
     * @param type        le type de document (PROGRAMME, ATTESTATION, etc.)
     * @return une réponse HTTP 201 contenant les métadonnées du document sauvegardé
     */
    @Operation(summary = "Uploader un document", description = "Permet d'ajouter un document à un événement scientifique. Stocke physiquement le fichier sur le serveur et enregistre ses métadonnées en base.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("fichier") MultipartFile fichier,
            @RequestParam(value = "evenementId", required = false) Long evenementId,
            @RequestParam("type") TypeDocument type) {

        DocumentResponse nouveauDocument = documentService.uploaderDocument(fichier, evenementId, type);
        return ResponseEntity.status(HttpStatus.CREATED).body(nouveauDocument);
    }

    @Operation(summary = "Lister les documents actifs", description = "Liste les métadonnées avec filtres optionnels par type, événement et année d'upload.")
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> listerDocuments(
            @RequestParam(required = false) TypeDocument type,
            @RequestParam(required = false) Long evenementId,
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(documentService.listerDocuments(type, evenementId, annee));
    }

    @Operation(summary = "Lister les documents archivés", description = "Réservé aux administrateurs et à la direction.")
    @GetMapping("/archives")
    public ResponseEntity<List<DocumentResponse>> listerArchives() {
        return ResponseEntity.ok(documentService.listerArchives());
    }

    @Operation(summary = "Archiver un document", description = "Archivage logique, réservé au déposant ou à la direction.")
    @DeleteMapping("/{id}")
    public ResponseEntity<DocumentResponse> archiverDocument(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.archiver(id));
    }

    @Operation(summary = "Restaurer un document", description = "Restaure un document sans effacer son historique d'archivage.")
    @PutMapping("/{id}/restaurer")
    public ResponseEntity<DocumentResponse> restaurerDocument(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.restaurer(id));
    }

    // =========================================================================
    // 2. CONSULTATION D'UN DOCUMENT PAR ID
    // =========================================================================

    /**
     * Endpoint pour récupérer les métadonnées d'un document spécifique.
     *
     * @param id l'identifiant du document
     * @return l'entité Document (HTTP 200) ou erreur 404 si introuvable
     */
    @Operation(summary = "Consulter un document par son ID", description = "Renvoie les métadonnées d'un document spécifique (nom, taille, type MIME...) sans charger le fichier physique.")
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.obtenirDocumentResponse(id));
    }

    // =========================================================================
    // 3. RECHERCHE PAR ÉVÉNEMENT
    // =========================================================================

    /**
     * Endpoint pour lister tous les documents rattachés à un événement.
     *
     * @param evenementId l'identifiant de l'événement
     * @return liste des documents de l'événement (HTTP 200)
     */
    @Operation(summary = "Lister les documents d'un événement", description = "Renvoie la liste des documents (programmes, rapports, etc.) rattachés à un événement spécifique.")
    @GetMapping("/evenement/{evenementId}")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByEvenement(@PathVariable Long evenementId) {
        return ResponseEntity.ok(documentService.obtenirDocumentsParEvenement(evenementId));
    }

    // =========================================================================
    // 4. TÉLÉCHARGEMENT DU FICHIER PHYSIQUE
    // =========================================================================

    /**
     * Endpoint pour télécharger le fichier physique d'un document.
     *
     * <p>Récupère la ressource physique depuis le service et configure les
     * headers HTTP appropriés (Content-Type et Content-Disposition) pour
     * déclencher le téléchargement côté client avec le nom d'origine.</p>
     *
     * @param id l'identifiant du document à télécharger
     * @return une réponse HTTP 200 contenant le flux binaire du fichier
     */
    @Operation(summary = "Télécharger un document", description = "Télécharge le fichier physique d'un document (binaire) avec son nom et son extension d'origine.")
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> telechargerFichier(@PathVariable Long id) {
        
        // 1. Récupérer les métadonnées pour connaître le nom et le type
        Document document = documentService.obtenirDocument(id);
        
        // 2. Récupérer le flux binaire du fichier
        Resource resource = documentService.chargerFichierPhysique(id);

        // 3. Déterminer le type MIME (ou octet-stream par défaut si non renseigné)
        MediaType contentType = MediaTypeFactory.getMediaType(document.getNomFichier())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        // 4. Configurer les en-têtes et renvoyer le fichier
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(document.getNomFichier(), StandardCharsets.UTF_8).build().toString())
                .body(resource);
    }
}
