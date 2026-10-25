package com.lias.gestion_laboratoire.controllers;

import com.lias.gestion_laboratoire.dto.GlobalSearchResult;
import com.lias.gestion_laboratoire.services.RechercheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST pour le module de Recherche et Consultation Globale.
 *
 * <p>Expose le point d'entrée unique permettant de lancer une recherche
 * plein texte et transversale à travers tout le système d'information
 * du laboratoire (Membres, Publications, Événements).</p>
 */
@RestController
@RequestMapping("/api/recherche")
@RequiredArgsConstructor
@Tag(name = "Recherche Globale", description = "Moteur de recherche transverse du laboratoire")
public class RechercheController {

    private final RechercheService rechercheService;

    /**
     * Exécute une recherche globale à partir d'un mot-clé.
     *
     * <p>Le paramètre {@code q} (pour query) est passé dans l'URL. Exemple :
     * {@code /api/recherche?q=intelligence}</p>
     *
     * @param keyword le mot-clé de recherche saisi par l'utilisateur
     * @return un objet {@link GlobalSearchResult} encapsulant les différentes entités trouvées
     */
    @Operation(summary = "Recherche globale", description = "Effectue une recherche par mot-clé (insensible à la casse) dans les Membres, Publications et Événements.")
    @GetMapping("/")
    public ResponseEntity<GlobalSearchResult> rechercher(@RequestParam("q") String keyword) {
        GlobalSearchResult resultat = rechercheService.executerRechercheGlobale(keyword);
        return ResponseEntity.ok(resultat);
    }
}
