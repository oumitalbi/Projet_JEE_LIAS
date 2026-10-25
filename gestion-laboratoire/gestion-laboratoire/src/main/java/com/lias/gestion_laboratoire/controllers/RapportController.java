package com.lias.gestion_laboratoire.controllers;

import com.lias.gestion_laboratoire.dto.RapportAnnuelDTO;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.services.RapportPdfService;
import com.lias.gestion_laboratoire.services.RapportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/rapports")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class RapportController {

    private final RapportService rapportService;
    private final RapportPdfService rapportPdfService;

    @GetMapping("/annuel/{annee}")
    public ResponseEntity<RapportAnnuelDTO> genererRapportAnnuel(
            @PathVariable int annee, @AuthenticationPrincipal Membre membre) {
        return ResponseEntity.ok(rapportService.genererRapportAnnuel(annee, membre));
    }

    @PostMapping("/annuel/{annee}")
    public ResponseEntity<RapportAnnuelDTO> creerRapportAnnuel(
            @PathVariable int annee, @AuthenticationPrincipal Membre membre) {
        return ResponseEntity.ok(rapportService.genererRapportAnnuel(annee, membre));
    }

    @GetMapping("/historique")
    public ResponseEntity<List<RapportAnnuelDTO>> obtenirHistorique(
            @AuthenticationPrincipal Membre membre) {
        return ResponseEntity.ok(rapportService.obtenirHistorique(membre));
    }

    @GetMapping("/historique/{id}")
    public ResponseEntity<RapportAnnuelDTO> obtenirArchive(
            @PathVariable Long id, @AuthenticationPrincipal Membre membre) {
        return ResponseEntity.ok(rapportService.obtenirArchive(id, membre));
    }

    @GetMapping(value = "/historique/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exporterPdf(
            @PathVariable Long id, @AuthenticationPrincipal Membre membre) {
        byte[] pdf = rapportPdfService.genererPdf(rapportService.obtenirArchive(id, membre));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("rapport-annuel-" + id + ".pdf", StandardCharsets.UTF_8).build().toString())
                .body(pdf);
    }
}
