package com.lias.gestion_laboratoire.controllers;

import com.lias.gestion_laboratoire.dto.ReunionRequest;
import com.lias.gestion_laboratoire.dto.ReunionResponse;
import com.lias.gestion_laboratoire.models.enums.StatutReunion;
import com.lias.gestion_laboratoire.services.ReunionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reunions")
@RequiredArgsConstructor
@Tag(name = "Réunions", description = "Gestion sécurisée des réunions et procès-verbaux")
public class ReunionController {

    private final ReunionService reunionService;

    @Operation(summary = "Lister les réunions visibles par le membre connecté")
    @GetMapping
    public ResponseEntity<List<ReunionResponse>> listerReunions(
            @RequestParam(defaultValue = "false") boolean archivees) {
        return ResponseEntity.ok(reunionService.listerReunions(archivees));
    }

    @Operation(summary = "Planifier une réunion")
    @PostMapping({ "", "/" })
    public ResponseEntity<ReunionResponse> planifierReunion(@Valid @RequestBody ReunionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reunionService.planifierReunion(request));
    }

    @Operation(summary = "Consulter une réunion visible par le membre connecté")
    @GetMapping("/{id}")
    public ResponseEntity<ReunionResponse> obtenirReunion(@PathVariable Long id) {
        return ResponseEntity.ok(reunionService.obtenirReunion(id));
    }

    @Operation(summary = "Modifier une réunion")
    @PutMapping("/{id}")
    public ResponseEntity<ReunionResponse> modifierReunion(
            @PathVariable Long id,
            @Valid @RequestBody ReunionRequest request) {
        return ResponseEntity.ok(reunionService.modifierReunion(id, request));
    }

    @Operation(summary = "Ajouter un participant")
    @PutMapping("/{id}/participants/{membreId}")
    public ResponseEntity<ReunionResponse> ajouterParticipant(
            @PathVariable Long id,
            @PathVariable Long membreId) {
        return ResponseEntity.ok(reunionService.ajouterParticipant(id, membreId));
    }

    @Operation(summary = "Retirer un participant")
    @DeleteMapping("/{id}/participants/{membreId}")
    public ResponseEntity<ReunionResponse> retirerParticipant(
            @PathVariable Long id,
            @PathVariable Long membreId) {
        return ResponseEntity.ok(reunionService.retirerParticipant(id, membreId));
    }

    @Operation(summary = "Changer le statut d'une réunion")
    @PutMapping("/{id}/statut")
    public ResponseEntity<ReunionResponse> changerStatut(
            @PathVariable Long id,
            @RequestParam StatutReunion statut) {
        return ResponseEntity.ok(reunionService.changerStatut(id, statut));
    }

    @Operation(summary = "Archiver une réunion sans supprimer ses informations ni son PV")
    @PutMapping("/{id}/archiver")
    public ResponseEntity<ReunionResponse> archiver(@PathVariable Long id) {
        return ResponseEntity.ok(reunionService.archiver(id));
    }

    @Operation(summary = "Restaurer une réunion archivée")
    @PutMapping("/{id}/desarchiver")
    public ResponseEntity<ReunionResponse> desarchiver(@PathVariable Long id) {
        return ResponseEntity.ok(reunionService.desarchiver(id));
    }

    @Operation(summary = "Attacher un document de type PV")
    @PutMapping("/{id}/pv/{documentId}")
    public ResponseEntity<ReunionResponse> attacherPV(
            @PathVariable Long id,
            @PathVariable Long documentId) {
        return ResponseEntity.ok(reunionService.attacherPV(id, documentId));
    }

    @Operation(summary = "Lister les réunions du membre demandé; l'accès est vérifié par le JWT")
    @GetMapping("/membre/{membreId}")
    public ResponseEntity<List<ReunionResponse>> obtenirReunionsMembre(@PathVariable Long membreId) {
        return ResponseEntity.ok(reunionService.obtenirReunionsMembre(membreId));
    }
}
