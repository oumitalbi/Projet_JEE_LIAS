package com.lias.gestion_laboratoire.controllers;

import com.lias.gestion_laboratoire.dtos.DemandeAdhesionReceipt;
import com.lias.gestion_laboratoire.dtos.DemandeAdhesionResponse;
import com.lias.gestion_laboratoire.dtos.DemandeCreateRequest;
import com.lias.gestion_laboratoire.dtos.TraitementRequest;
import com.lias.gestion_laboratoire.dtos.TraitementAdhesionResponse;
import com.lias.gestion_laboratoire.models.DemandeAdhesion;
import com.lias.gestion_laboratoire.models.enums.StatutAdhesion;
import com.lias.gestion_laboratoire.services.AdhesionRateLimiter;
import com.lias.gestion_laboratoire.services.DemandeAdhesionService;
import com.lias.gestion_laboratoire.services.PrivateAdhesionCvStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/adhesions")
@RequiredArgsConstructor
@Tag(name = "Adhésions", description = "Candidatures, décisions et archivage")
public class DemandeAdhesionController {
    private final DemandeAdhesionService demandeAdhesionService;
    private final AdhesionRateLimiter rateLimiter;
    private final PrivateAdhesionCvStorage cvStorage;

    @PostMapping(value = "/soumettre", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Soumettre une candidature publique avec CV PDF")
    public ResponseEntity<DemandeAdhesionReceipt> soumettreDemande(
            @Valid @ModelAttribute DemandeCreateRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        rateLimiter.verifyAndConsume(servletRequest.getRemoteAddr());
        DemandeAdhesion demande = demandeAdhesionService.soumettreDemande(request);
        return ResponseEntity.status(201).body(new DemandeAdhesionReceipt(
                demande.getId(), StatutAdhesion.EN_ATTENTE, demande.getDateDemande(),
                "Votre candidature a été transmise au laboratoire."));
    }

    @GetMapping("/en-attente")
    @PreAuthorize("hasAuthority('DIRECTEUR') or hasAuthority('ADMIN')")
    @Operation(summary = "Lister les demandes en attente")
    public ResponseEntity<List<DemandeAdhesionResponse>> obtenirDemandesEnAttente() {
        return ResponseEntity.ok(demandeAdhesionService.obtenirDemandesEnAttente());
    }

    @GetMapping("/historique")
    @PreAuthorize("hasAuthority('DIRECTEUR') or hasAuthority('ADMIN')")
    @Operation(summary = "Lister les demandes déjà traitées")
    public ResponseEntity<List<DemandeAdhesionResponse>> obtenirHistorique() {
        return ResponseEntity.ok(demandeAdhesionService.obtenirHistorique());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DIRECTEUR') or hasAuthority('ADMIN')")
    @Operation(summary = "Consulter le détail d'une candidature")
    public ResponseEntity<DemandeAdhesionResponse> obtenirDemande(@PathVariable Long id) {
        return ResponseEntity.ok(demandeAdhesionService.obtenirDemande(id));
    }

    @PutMapping("/{id}/traiter")
    @PreAuthorize("hasAuthority('DIRECTEUR') or hasAuthority('ADMIN')")
    @Operation(summary = "Accepter ou refuser une candidature")
    public ResponseEntity<TraitementAdhesionResponse> traiterDemande(
            @PathVariable Long id, @Valid @RequestBody TraitementRequest request) {
        return ResponseEntity.ok(demandeAdhesionService.traiterDemande(id, request));
    }

    @GetMapping("/{id}/cv")
    @PreAuthorize("hasAuthority('DIRECTEUR') or hasAuthority('ADMIN')")
    @Operation(summary = "Télécharger le CV privé d'une candidature")
    public ResponseEntity<Resource> telechargerCv(@PathVariable Long id) {
        String storedPath = demandeAdhesionService.obtenirCheminCv(id);
        Resource resource = cvStorage.load(storedPath);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cv-adhesion-" + id + ".pdf\"")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, private")
                .body(resource);
    }
}
