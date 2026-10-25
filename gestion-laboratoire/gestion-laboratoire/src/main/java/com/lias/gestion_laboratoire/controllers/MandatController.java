package com.lias.gestion_laboratoire.controllers;

import com.lias.gestion_laboratoire.dto.MandatClotureRequest;
import com.lias.gestion_laboratoire.dto.MandatRequest;
import com.lias.gestion_laboratoire.dto.MandatResponse;
import com.lias.gestion_laboratoire.dto.MandatUpdateRequest;
import com.lias.gestion_laboratoire.dto.HistoriqueRoleResponse;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.services.MandatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mandats")
@RequiredArgsConstructor
@Tag(name = "Mandats", description = "Gouvernance, mandats et historiques")
public class MandatController {
    private final MandatService mandatService;

    @Value("${app.laboratoire.date-creation:}")
    private String dateCreationLaboratoire;

    @GetMapping("/actifs")
    @Operation(summary = "Lister les mandats en cours")
    public ResponseEntity<List<MandatResponse>> obtenirMandatsActifs() {
        return ResponseEntity.ok(mandatService.obtenirMandatsActifs());
    }

    @GetMapping
    @Operation(summary = "Lister l'historique complet des mandats")
    public ResponseEntity<List<MandatResponse>> obtenirTousLesMandats() {
        return ResponseEntity.ok(mandatService.obtenirTousLesMandats());
    }

    @GetMapping("/date-creation-laboratoire")
    public ResponseEntity<Map<String, String>> obtenirDateCreationLaboratoire() {
        return ResponseEntity.ok(Map.of("dateCreation", dateCreationLaboratoire));
    }

    @GetMapping("/equipe/{equipe}")
    public ResponseEntity<List<MandatResponse>> obtenirMandatsEquipe(@PathVariable EquipeLaboratoire equipe) {
        return ResponseEntity.ok(mandatService.obtenirMandatsEquipe(equipe));
    }

    @GetMapping("/membre/{membreId}")
    @Operation(summary = "Historique des mandats d'un membre")
    public ResponseEntity<List<MandatResponse>> obtenirHistoriqueMembre(@PathVariable Long membreId) {
        return ResponseEntity.ok(mandatService.obtenirHistoriqueMembre(membreId));
    }

    @GetMapping("/historique-roles/{membreId}")
    public ResponseEntity<List<HistoriqueRoleResponse>> obtenirHistoriqueRoles(@PathVariable Long membreId) {
        return ResponseEntity.ok(mandatService.obtenirHistoriqueRoles(membreId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MandatResponse> obtenirMandat(@PathVariable Long id) {
        return ResponseEntity.ok(mandatService.obtenirMandat(id));
    }

    @PostMapping
    @Operation(summary = "Attribuer un mandat")
    public ResponseEntity<MandatResponse> assignerMandat(@Valid @RequestBody MandatRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mandatService.assignerNouveauMandat(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Corriger un mandat actif")
    public ResponseEntity<MandatResponse> modifierMandat(@PathVariable Long id,
                                                         @Valid @RequestBody MandatUpdateRequest request) {
        return ResponseEntity.ok(mandatService.modifierMandat(id, request));
    }

    @PutMapping("/{id}/cloturer")
    @Operation(summary = "Clôturer un mandat")
    public ResponseEntity<MandatResponse> cloturerMandat(@PathVariable Long id,
                                                         @Valid @RequestBody MandatClotureRequest request) {
        return ResponseEntity.ok(mandatService.cloturerMandat(id, request));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleEntityNotFoundException(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
