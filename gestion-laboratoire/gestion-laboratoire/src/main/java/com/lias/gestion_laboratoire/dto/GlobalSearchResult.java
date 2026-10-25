package com.lias.gestion_laboratoire.dto;

import com.lias.gestion_laboratoire.models.Evenement;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.Publication;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Objet de transfert de données (DTO) encapsulant les résultats d'une recherche 
 * globale par mot-clé à travers l'ensemble du système d'information du laboratoire.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchResult {

    /**
     * Liste des membres dont le nom, prénom ou biographie contient le mot-clé.
     */
    private List<Membre> membres;

    /**
     * Liste des publications dont le titre ou le résumé contient le mot-clé.
     */
    private List<Publication> publications;

    /**
     * Liste des événements dont le titre, la description ou le lieu contient le mot-clé.
     */
    private List<Evenement> evenements;

    private List<DocumentResponse> documents;
}
