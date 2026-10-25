package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.dto.HistoriqueModificationProfilResponse;
import com.lias.gestion_laboratoire.models.HistoriqueModificationProfil;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.repositories.HistoriqueModificationProfilRepository;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class HistoriqueModificationProfilService {
    private final HistoriqueModificationProfilRepository historiqueRepository;
    private final MembreRepository membreRepository;

    public void enregistrer(Membre membre, String action, String details) {
        HistoriqueModificationProfil historique = new HistoriqueModificationProfil();
        historique.setMembre(membre);
        historique.setAction(action);
        historique.setDetails(details);
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Membre auteur) {
            historique.setEffectuePar(membreRepository.findById(auteur.getId()).orElse(null));
        }
        historiqueRepository.save(historique);
    }

    @Transactional(readOnly = true)
    public List<HistoriqueModificationProfilResponse> obtenirHistorique(Long membreId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Membre auteur) || !auteur.isCompteActif() || auteur.isCompteGele()
                || auteur.getStatut() == Statut.DOCTORANT || auteur.getStatut() == Statut.RETRAITE
                || auteur.getStatut() == Statut.ANCIEN) {
            throw new AccessDeniedException("Un membre actif doit être connecté.");
        }
        if (!membreRepository.existsById(membreId)) {
            throw new EntityNotFoundException("Membre introuvable : " + membreId);
        }
        return historiqueRepository.findByMembreIdOrderByDateActionDesc(membreId).stream()
                .map(HistoriqueModificationProfilResponse::from).toList();
    }
}
