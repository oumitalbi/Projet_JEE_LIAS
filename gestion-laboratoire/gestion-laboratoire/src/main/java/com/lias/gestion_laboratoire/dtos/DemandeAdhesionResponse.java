package com.lias.gestion_laboratoire.dtos;

import com.lias.gestion_laboratoire.models.DemandeAdhesion;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.models.enums.StatutAdhesion;

import java.time.LocalDateTime;

public record DemandeAdhesionResponse(
        Long id,
        String nom,
        String prenom,
        String email,
        Statut statutSouhaite,
        String etablissementOrigine,
        String laboratoireOrigine,
        EquipeLaboratoire equipeSouhaitee,
        String lettreMotivation,
        String centresInteret,
        String biographie,
        boolean consentementDonnees,
        LocalDateTime consentementDate,
        StatutAdhesion statut,
        LocalDateTime dateDemande,
        LocalDateTime dateTraitement,
        String motifDecision,
        MembreResume traitePar,
        MembreResume membreCree,
        boolean cvDisponible,
        String lienTelechargementCv) {

    public static DemandeAdhesionResponse from(DemandeAdhesion demande) {
        return new DemandeAdhesionResponse(
                demande.getId(), demande.getNom(), demande.getPrenom(), demande.getEmail(),
                demande.getStatutSouhaite(), demande.getEtablissementOrigine(), demande.getLaboratoireOrigine(),
                demande.getEquipeSouhaitee(), demande.getLettreMotivation(), demande.getCentresInteret(),
                demande.getBiographie(), demande.isConsentementDonnees(), demande.getConsentementDate(),
                demande.getStatut(), demande.getDateDemande(), demande.getDateTraitement(),
                demande.getMotifDecision(), MembreResume.from(demande.getTraitePar()),
                MembreResume.from(demande.getMembreCree()), demande.getCvChemin() != null,
                demande.getCvChemin() == null ? null : "/api/adhesions/" + demande.getId() + "/cv");
    }

    public record MembreResume(Long id, String nom, String prenom) {
        static MembreResume from(com.lias.gestion_laboratoire.models.Membre membre) {
            return membre == null ? null : new MembreResume(membre.getId(), membre.getNom(), membre.getPrenom());
        }
    }
}
