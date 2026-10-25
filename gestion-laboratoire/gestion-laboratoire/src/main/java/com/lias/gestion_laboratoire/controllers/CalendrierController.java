package com.lias.gestion_laboratoire.controllers;

import com.lias.gestion_laboratoire.dto.CalendrierItemResponse;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.services.CalendrierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calendrier")
@RequiredArgsConstructor
public class CalendrierController {

    private final CalendrierService calendrierService;

    @GetMapping("/mois")
    public ResponseEntity<?> obtenirEvenementsParMois(
            @RequestParam int annee,
            @RequestParam int mois,
            @AuthenticationPrincipal Membre membre) {

        if (mois < 1 || mois > 12) {
            return ResponseEntity.badRequest().body("Le mois doit être compris entre 1 et 12.");
        }
        if (annee < 1 || annee > 9999) {
            return ResponseEntity.badRequest().body("L'année doit être comprise entre 1 et 9999.");
        }

        List<CalendrierItemResponse> evenements = calendrierService.obtenirEvenementsParMois(annee, mois, membre);
        return ResponseEntity.ok(evenements);
    }
}
