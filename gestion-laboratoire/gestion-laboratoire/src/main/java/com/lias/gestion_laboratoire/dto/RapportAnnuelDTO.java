package com.lias.gestion_laboratoire.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RapportAnnuelDTO {
    private Long id;
    private int annee;
    private int version;
    private LocalDateTime dateGeneration;
    private String generePar;
    private long nbEvenements;
    private long nbPublications;
    private long nbReunions;
    private long nbConventionsActives;
    private long nbDemandesLogistiques;
    private long nbMandats;
    private long nbNouveauxMembres;
    private long nbDemandesAdhesion;
    private Map<String, Long> evenementsParType;
    private Map<String, Long> publicationsParType;
    private Map<String, Long> publicationsParEquipe;
    private Map<String, Long> logistiqueParStatut;
    private Map<String, Long> mandatsParRole;
    private Map<String, Long> membresParStatut;
    private Map<String, Long> adhesionsParDecision;
}
