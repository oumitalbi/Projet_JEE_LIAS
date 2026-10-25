package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.dto.RapportAnnuelDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RapportPdfServiceTests {

    @Test
    void exportsArchivedReportAsPdf() {
        RapportAnnuelDTO report = RapportAnnuelDTO.builder()
                .annee(2026)
                .version(1)
                .dateGeneration(LocalDateTime.of(2026, 10, 23, 12, 0))
                .generePar("Directeur LIAS")
                .nbEvenements(2)
                .nbPublications(3)
                .nbReunions(1)
                .nbConventionsActives(1)
                .nbDemandesLogistiques(0)
                .nbMandats(2)
                .nbNouveauxMembres(1)
                .nbDemandesAdhesion(2)
                .evenementsParType(Map.of("CONFERENCE", 2L))
                .publicationsParType(Map.of("ARTICLE_JOURNAL", 3L))
                .publicationsParEquipe(Map.of("ISDIAC", 3L))
                .logistiqueParStatut(Map.of())
                .mandatsParRole(Map.of("DIRECTEUR", 1L))
                .membresParStatut(Map.of("PERMANENT", 1L))
                .adhesionsParDecision(Map.of("ACCEPTEE", 1L))
                .build();

        byte[] pdf = new RapportPdfService().genererPdf(report);

        assertTrue(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF"));
    }
}
