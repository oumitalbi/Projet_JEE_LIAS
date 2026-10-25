package com.lias.gestion_laboratoire.controllers;

import com.lias.gestion_laboratoire.dto.EvenementAnnulationRequest;
import com.lias.gestion_laboratoire.dto.EvenementPublicResponse;
import com.lias.gestion_laboratoire.dto.EvenementRequest;
import com.lias.gestion_laboratoire.dto.EvenementResponse;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.TypeEvenement;
import com.lias.gestion_laboratoire.services.EvenementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evenements")
@RequiredArgsConstructor
public class EvenementController {

    private final EvenementService evenementService;

    @GetMapping("/public/a-venir")
    public ResponseEntity<List<EvenementPublicResponse>> obtenirEvenementsPublicsAVenir() {
        return ResponseEntity.ok(evenementService.obtenirEvenementsPublicsAVenir());
    }

    @GetMapping
    public ResponseEntity<List<EvenementResponse>> obtenirTousLesEvenements(
            @AuthenticationPrincipal Membre utilisateur) {
        return ResponseEntity.ok(evenementService.obtenirTousLesEvenements(utilisateur));
    }

    @GetMapping("/a-venir")
    public ResponseEntity<List<EvenementResponse>> obtenirEvenementsAVenir(
            @AuthenticationPrincipal Membre utilisateur) {
        return ResponseEntity.ok(evenementService.obtenirEvenementsAVenir(utilisateur));
    }

    @GetMapping("/annules")
    public ResponseEntity<List<EvenementResponse>> obtenirEvenementsAnnules(
            @AuthenticationPrincipal Membre utilisateur) {
        return ResponseEntity.ok(evenementService.obtenirEvenementsAnnules(utilisateur));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<EvenementResponse>> obtenirEvenementsParType(
            @PathVariable TypeEvenement type,
            @AuthenticationPrincipal Membre utilisateur) {
        return ResponseEntity.ok(evenementService.obtenirEvenementsParType(type, utilisateur));
    }

    @GetMapping("/participant/{membreId}")
    public ResponseEntity<List<EvenementResponse>> obtenirEvenementsParParticipant(
            @PathVariable Long membreId,
            @AuthenticationPrincipal Membre utilisateur) {
        return ResponseEntity.ok(evenementService.obtenirEvenementsParParticipant(membreId, utilisateur));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvenementResponse> obtenirEvenement(
            @PathVariable Long id,
            @AuthenticationPrincipal Membre utilisateur) {
        return ResponseEntity.ok(evenementService.obtenirEvenement(id, utilisateur));
    }

    @PostMapping
    public ResponseEntity<EvenementResponse> creerEvenement(
            @Valid @RequestBody EvenementRequest request,
            @AuthenticationPrincipal Membre utilisateur) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(evenementService.creerEvenement(request, utilisateur));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EvenementResponse> modifierEvenement(
            @PathVariable Long id,
            @Valid @RequestBody EvenementRequest request,
            @AuthenticationPrincipal Membre utilisateur) {
        return ResponseEntity.ok(evenementService.modifierEvenement(id, request, utilisateur));
    }

    @PutMapping("/{id}/organisateurs/{membreId}")
    public ResponseEntity<EvenementResponse> affecterOrganisateur(
            @PathVariable Long id,
            @PathVariable Long membreId,
            @AuthenticationPrincipal Membre utilisateur) {
        return ResponseEntity.ok(evenementService.affecterOrganisateur(id, membreId, utilisateur));
    }

    @DeleteMapping("/{id}/organisateurs/{membreId}")
    public ResponseEntity<EvenementResponse> retirerOrganisateur(
            @PathVariable Long id,
            @PathVariable Long membreId,
            @AuthenticationPrincipal Membre utilisateur) {
        return ResponseEntity.ok(evenementService.retirerOrganisateur(id, membreId, utilisateur));
    }

    @PostMapping("/{id}/inscription")
    public ResponseEntity<EvenementResponse> inscrire(
            @PathVariable Long id,
            @AuthenticationPrincipal Membre utilisateur) {
        return ResponseEntity.ok(evenementService.inscrire(id, utilisateur));
    }

    @DeleteMapping("/{id}/inscription")
    public ResponseEntity<EvenementResponse> desinscrire(
            @PathVariable Long id,
            @AuthenticationPrincipal Membre utilisateur) {
        return ResponseEntity.ok(evenementService.desinscrire(id, utilisateur));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<EvenementResponse> annulerEvenement(
            @PathVariable Long id,
            @Valid @RequestBody EvenementAnnulationRequest request,
            @AuthenticationPrincipal Membre utilisateur) {
        return ResponseEntity.ok(evenementService.annulerEvenement(id, request.motif(), utilisateur));
    }
}
