package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.DemandeMateriel;
import com.lias.gestion_laboratoire.models.Membre;

import java.time.LocalDateTime;

public record DemandeMaterielResponse(
        Long id,
        String descriptionBesoin,
        String nomBesoin,
        Integer quantiteDemandee,
        Integer quantiteRetournee,
        String motifDemande,
        String motifRefus,
        String statut,
        LocalDateTime dateDemande,
        LocalDateTime dateReponse,
        LocalDateTime dateRetour,
        MemberSummary demandeur,
        MemberSummary traitePar,
        MemberSummary retournePar,
        MaterielSummary materiel,
        boolean peutValider,
        boolean peutRetourner) {

    public static DemandeMaterielResponse from(DemandeMateriel request, boolean peutValider,
                                               boolean peutRetourner) {
        return new DemandeMaterielResponse(request.getId(), request.getDescriptionBesoin(),
                request.getNomBesoin(), request.getQuantiteDemandee(), request.getQuantiteRetournee(),
                request.getMotifDemande(), request.getMotifRefus(),
                request.getStatut() == null ? null : request.getStatut().name(),
                request.getDateDemande(), request.getDateReponse(), request.getDateRetour(),
                MemberSummary.from(request.getDemandeur()), MemberSummary.from(request.getTraitePar()),
                MemberSummary.from(request.getRetournePar()),
                MaterielSummary.from(request.getMaterielAssigne() != null
                        ? request.getMaterielAssigne() : request.getMaterielDemande()),
                peutValider, peutRetourner);
    }

    public record MemberSummary(Long id, String nom, String prenom, String photo, String equipe) {
        private static MemberSummary from(Membre member) {
            return member == null ? null : new MemberSummary(member.getId(), member.getNom(), member.getPrenom(),
                    member.getPhoto(), member.getEquipeLaboratoire() == null ? null : member.getEquipeLaboratoire().name());
        }
    }

    public record MaterielSummary(Long id, String nom, String categorie, boolean consommable) {
        private static MaterielSummary from(com.lias.gestion_laboratoire.models.Materiel materiel) {
            return materiel == null ? null : new MaterielSummary(materiel.getId(), materiel.getNom(),
                    materiel.getCategorie(), materiel.isConsommable());
        }
    }
}
