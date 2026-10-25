package com.lias.gestion_laboratoire.controllers;

import com.lias.gestion_laboratoire.dto.*;
import com.lias.gestion_laboratoire.models.enums.StatutDemande;
import com.lias.gestion_laboratoire.services.LogistiqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logistique")
@RequiredArgsConstructor
@Tag(name = "Logistique", description = "Gestion du matériel, des arrivages et des demandes")
public class LogistiqueController {
    private final LogistiqueService logistiqueService;

    @Operation(summary = "Ajouter un matériel (directeur uniquement)")
    @PostMapping("/materiel")
    public ResponseEntity<MaterielResponse> ajouterMateriel(@Valid @RequestBody MaterielRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(logistiqueService.ajouterMateriel(request));
    }

    @Operation(summary = "Lister le catalogue complet")
    @GetMapping("/materiel")
    public ResponseEntity<List<MaterielResponse>> obtenirMateriel(
            @RequestParam(defaultValue = "false") boolean archives) {
        return ResponseEntity.ok(logistiqueService.obtenirMateriel(archives, false));
    }

    @Operation(summary = "Lister le matériel disponible")
    @GetMapping("/materiel/disponible")
    public ResponseEntity<List<MaterielResponse>> obtenirMaterielDisponible() {
        return ResponseEntity.ok(logistiqueService.obtenirMateriel(false, true));
    }

    @Operation(summary = "Consulter un matériel")
    @GetMapping("/materiel/{id}")
    public ResponseEntity<MaterielResponse> obtenirMateriel(@PathVariable Long id) {
        return ResponseEntity.ok(logistiqueService.obtenirMateriel(id));
    }

    @Operation(summary = "Modifier un matériel (directeur uniquement)")
    @PutMapping("/materiel/{id}")
    public ResponseEntity<MaterielResponse> modifierMateriel(
            @PathVariable Long id, @Valid @RequestBody MaterielRequest request) {
        return ResponseEntity.ok(logistiqueService.modifierMateriel(id, request));
    }

    @Operation(summary = "Archiver logiquement un matériel")
    @DeleteMapping("/materiel/{id}")
    public ResponseEntity<MaterielResponse> archiverMateriel(@PathVariable Long id) {
        return ResponseEntity.ok(logistiqueService.archiverMateriel(id, true));
    }

    @Operation(summary = "Restaurer un matériel")
    @PutMapping("/materiel/{id}/restaurer")
    public ResponseEntity<MaterielResponse> restaurerMateriel(@PathVariable Long id) {
        return ResponseEntity.ok(logistiqueService.archiverMateriel(id, false));
    }

    @Operation(summary = "Soumettre une demande avec identité tirée du JWT")
    @PostMapping("/demandes")
    public ResponseEntity<DemandeMaterielResponse> soumettreDemande(
            @Valid @RequestBody DemandeMaterielRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(logistiqueService.soumettreDemande(request));
    }

    @Operation(summary = "Lister mes demandes ou la file de validation")
    @GetMapping("/demandes")
    public ResponseEntity<List<DemandeMaterielResponse>> listerDemandes(
            @RequestParam(defaultValue = "true") boolean miennes,
            @RequestParam(required = false) StatutDemande statut) {
        return ResponseEntity.ok(logistiqueService.listerDemandes(miennes, statut));
    }

    @Operation(summary = "Consulter une demande")
    @GetMapping("/demandes/{id}")
    public ResponseEntity<DemandeMaterielResponse> obtenirDemande(@PathVariable Long id) {
        return ResponseEntity.ok(logistiqueService.obtenirDemande(id));
    }

    @Operation(summary = "Valider une demande et attribuer le matériel disponible")
    @PutMapping("/demandes/{id}/valider")
    public ResponseEntity<DemandeMaterielResponse> validerDemande(
            @PathVariable Long id,
            @RequestParam(required = false) Long materielId) {
        return ResponseEntity.ok(logistiqueService.validerDemande(id, materielId));
    }

    @Operation(summary = "Refuser une demande avec motif obligatoire")
    @PutMapping("/demandes/{id}/refuser")
    public ResponseEntity<DemandeMaterielResponse> refuserDemande(
            @PathVariable Long id, @Valid @RequestBody RefusDemandeRequest request) {
        return ResponseEntity.ok(logistiqueService.refuserDemande(id, request.motif()));
    }

    @Operation(summary = "Enregistrer le retour intégral d'un matériel prêté")
    @PutMapping("/demandes/{id}/retourner")
    public ResponseEntity<DemandeMaterielResponse> retournerMateriel(@PathVariable Long id) {
        return ResponseEntity.ok(logistiqueService.retournerMateriel(id));
    }

    @Operation(summary = "Enregistrer un arrivage (directeur uniquement)")
    @PostMapping("/arrivages")
    public ResponseEntity<ArrivageMaterielResponse> enregistrerArrivage(
            @Valid @RequestBody ArrivageMaterielRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(logistiqueService.enregistrerArrivage(request));
    }

    @Operation(summary = "Consulter l'historique des arrivages")
    @GetMapping("/arrivages")
    public ResponseEntity<List<ArrivageMaterielResponse>> listerArrivages(
            @RequestParam(required = false) Long materielId) {
        return ResponseEntity.ok(logistiqueService.listerArrivages(materielId));
    }

    @Operation(summary = "Indicateur de distribution par équipe (directeur uniquement)")
    @GetMapping("/distributions/equite")
    public ResponseEntity<List<LogistiqueService.DistributionEquipeResponse>> indicateursEquite() {
        return ResponseEntity.ok(logistiqueService.indicateursEquite());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }
}
