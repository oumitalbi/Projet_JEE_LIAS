package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.dto.GlobalSearchResult;
import com.lias.gestion_laboratoire.repositories.EvenementRepository;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import com.lias.gestion_laboratoire.repositories.PublicationRepository;
import com.lias.gestion_laboratoire.authorization.AuthorizationService;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.dto.DocumentResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Service métier pour le Module 18 : Recherche et consultation globale.
 *
 * <p>Ce service centralise les appels vers différents repositories afin 
 * d'offrir une fonctionnalité de recherche par mot-clé "plein texte" 
 * agnostique à travers tout le système (Membres, Publications, Événements).</p>
 */
@Service
@RequiredArgsConstructor
public class RechercheService {

    private final MembreRepository membreRepository;
    private final PublicationRepository publicationRepository;
    private final EvenementRepository evenementRepository;
    private final DocumentService documentService;
    private final AuthorizationService authorizationService;

    /**
     * Exécute une recherche globale par mot-clé sur les entités principales du laboratoire.
     *
     * <p>Si le mot-clé est null, vide ou comporte moins de 2 caractères, la recherche
     * est avortée par mesure de performance et des listes vides sont retournées.</p>
     *
     * @param keyword le mot-clé saisi par l'utilisateur
     * @return un objet {@link GlobalSearchResult} contenant les listes d'entités correspondantes
     */
    @Transactional(readOnly = true)
    public GlobalSearchResult executerRechercheGlobale(String keyword) {
        
        // Règle métier : Éviter les requêtes trop lourdes sur des mots trop courts
        if (keyword == null || keyword.trim().length() < 2) {
            return new GlobalSearchResult(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        String searchKeyword = keyword.trim();

        // 1. Recherche dans les Membres (Nom, Prénom, Biographie)
        var membres = membreRepository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCaseOrBiographieContainingIgnoreCase(
                searchKeyword, searchKeyword, searchKeyword
        );

        // 2. Recherche dans les Publications (Titre, Résumé)
        var publications = publicationRepository.findByTitreContainingIgnoreCaseOrResumeContainingIgnoreCase(
                searchKeyword, searchKeyword
        );

        // 3. Recherche dans les Événements (Titre, Description, Lieu)
        var evenements = evenementRepository.findByTitreContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrLieuContainingIgnoreCase(
                searchKeyword, searchKeyword, searchKeyword
        );

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        List<DocumentResponse> documents = principal instanceof Membre membre
                && authorizationService.peutConsulterDocuments(membre)
                ? documentService.rechercherDocumentsPourRechercheGlobale(searchKeyword, membre)
                : Collections.emptyList();

        return new GlobalSearchResult(membres, publications, evenements, documents);
    }
}
