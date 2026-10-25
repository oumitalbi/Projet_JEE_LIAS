package com.lias.gestion_laboratoire.controllers;

import com.lias.gestion_laboratoire.dto.ConventionRequest;
import com.lias.gestion_laboratoire.dto.ConventionResponse;
import com.lias.gestion_laboratoire.dto.PartenaireRequest;
import com.lias.gestion_laboratoire.dto.PartenaireResponse;
import com.lias.gestion_laboratoire.services.ConventionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conventions")
@RequiredArgsConstructor
@Tag(name = "Conventions", description = "Gestion sécurisée des partenaires et conventions")
public class ConventionController {

    private final ConventionService conventionService;

    @Operation(summary = "Lister les partenaires")
    @GetMapping("/partenaires")
    public ResponseEntity<List<PartenaireResponse>> listerPartenaires(
            @RequestParam(defaultValue = "false") boolean archives) {
        return ResponseEntity.ok(conventionService.listerPartenaires(archives));
    }

    @Operation(summary = "Ajouter un partenaire")
    @PostMapping("/partenaires")
    public ResponseEntity<PartenaireResponse> ajouterPartenaire(@Valid @RequestBody PartenaireRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conventionService.ajouterPartenaire(request));
    }

    @Operation(summary = "Modifier un partenaire")
    @PutMapping("/partenaires/{id}")
    public ResponseEntity<PartenaireResponse> modifierPartenaire(
            @PathVariable Long id,
            @Valid @RequestBody PartenaireRequest request) {
        return ResponseEntity.ok(conventionService.modifierPartenaire(id, request));
    }

    @Operation(summary = "Archiver un partenaire sans le supprimer")
    @DeleteMapping("/partenaires/{id}")
    public ResponseEntity<PartenaireResponse> archiverPartenaire(@PathVariable Long id) {
        return ResponseEntity.ok(conventionService.archiverPartenaire(id));
    }

    @Operation(summary = "Restaurer un partenaire archivé")
    @PutMapping("/partenaires/{id}/restaurer")
    public ResponseEntity<PartenaireResponse> restaurerPartenaire(@PathVariable Long id) {
        return ResponseEntity.ok(conventionService.restaurerPartenaire(id));
    }

    @Operation(summary = "Lister les conventions avec filtres")
    @GetMapping({"", "/"})
    public ResponseEntity<List<ConventionResponse>> listerConventions(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Long partenaireId,
            @RequestParam(required = false) Integer annee,
            @RequestParam(defaultValue = "false") boolean archives) {
        return ResponseEntity.ok(conventionService.listerConventions(statut, partenaireId, annee, archives));
    }

    @Operation(summary = "Lister les conventions actives")
    @GetMapping("/actives")
    public ResponseEntity<List<ConventionResponse>> obtenirConventionsActives() {
        return ResponseEntity.ok(conventionService.obtenirConventionsActives());
    }

    @Operation(summary = "Créer une convention")
    @PostMapping({"", "/"})
    public ResponseEntity<ConventionResponse> creerConvention(@Valid @RequestBody ConventionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conventionService.creerConvention(request));
    }

    @Operation(summary = "Consulter le détail d'une convention")
    @GetMapping("/{id}")
    public ResponseEntity<ConventionResponse> obtenirConvention(@PathVariable Long id) {
        return ResponseEntity.ok(conventionService.obtenirConvention(id));
    }

    @Operation(summary = "Modifier une convention")
    @PutMapping("/{id}")
    public ResponseEntity<ConventionResponse> modifierConvention(
            @PathVariable Long id,
            @Valid @RequestBody ConventionRequest request) {
        return ResponseEntity.ok(conventionService.modifierConvention(id, request));
    }

    @Operation(summary = "Archiver logiquement une convention")
    @DeleteMapping("/{id}")
    public ResponseEntity<ConventionResponse> archiverConvention(@PathVariable Long id) {
        return ResponseEntity.ok(conventionService.archiverConvention(id));
    }

    @Operation(summary = "Restaurer une convention archivée")
    @PutMapping("/{id}/desarchiver")
    public ResponseEntity<ConventionResponse> desarchiverConvention(@PathVariable Long id) {
        return ResponseEntity.ok(conventionService.desarchiverConvention(id));
    }

    @Operation(summary = "Attacher un document PDF de type CONVENTION")
    @PutMapping("/{id}/document/{documentId}")
    public ResponseEntity<ConventionResponse> attacherDocument(
            @PathVariable Long id,
            @PathVariable Long documentId) {
        return ResponseEntity.ok(conventionService.attacherDocument(id, documentId));
    }

    @Operation(summary = "Lier un événement comme activité associée")
    @PutMapping("/{id}/activites/{evenementId}")
    public ResponseEntity<ConventionResponse> ajouterActivite(
            @PathVariable Long id,
            @PathVariable Long evenementId) {
        return ResponseEntity.ok(conventionService.ajouterActivite(id, evenementId));
    }

    @Operation(summary = "Retirer le lien vers une activité")
    @DeleteMapping("/{id}/activites/{evenementId}")
    public ResponseEntity<ConventionResponse> retirerActivite(
            @PathVariable Long id,
            @PathVariable Long evenementId) {
        return ResponseEntity.ok(conventionService.retirerActivite(id, evenementId));
    }
}
