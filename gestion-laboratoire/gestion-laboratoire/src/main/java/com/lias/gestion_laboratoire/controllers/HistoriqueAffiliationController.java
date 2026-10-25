package com.lias.gestion_laboratoire.controllers;

import com.lias.gestion_laboratoire.models.HistoriqueAffiliation;
import com.lias.gestion_laboratoire.dto.PeriodeAffiliationResponse;
import com.lias.gestion_laboratoire.services.HistoriqueAffiliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST exposant les endpoints de consultation de l'historique
 * des affiliations des membres du laboratoire de recherche LIAS.
 *
 * <p>Base URL : {@code /api/affiliations}</p>
 *
 * @author LIAS Laboratory
 * @see HistoriqueAffiliationService
 */
@RestController
@RequestMapping("/api/affiliations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class HistoriqueAffiliationController {

    private final HistoriqueAffiliationService historiqueService;

    // =========================================================================
    // GET /api/affiliations/{membreId}
    // =========================================================================

    /**
     * Récupère l'historique complet des affiliations d'un membre du laboratoire.
     *
     * <p>Retourne toutes les périodes d'affiliation (actives et clôturées),
     * triées de la plus récente à la plus ancienne.</p>
     *
     * <p><strong>Exemple :</strong> {@code GET /api/affiliations/42}</p>
     *
     * @param membreId l'identifiant du membre
     * @return {@code 200 OK} avec la liste des périodes d'affiliation
     */
    @GetMapping("/{membreId}")
    public ResponseEntity<List<PeriodeAffiliationResponse>> obtenirHistorique(
            @PathVariable("membreId") Long membreId) {
        List<HistoriqueAffiliation> historique =
                historiqueService.obtenirHistoriqueParMembre(membreId);
        return ResponseEntity.ok(historique.stream().map(PeriodeAffiliationResponse::from).toList());
    }
}
