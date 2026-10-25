package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.Document;
import com.lias.gestion_laboratoire.models.enums.TypeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité {@link Document}.
 *
 * <p>Ce repository fournit l'ensemble des opérations CRUD standard via
 * {@link JpaRepository} ainsi que des méthodes de recherche personnalisées
 * adaptées aux besoins du module d'archivage documentaire du laboratoire LIAS.</p>
 *
 * @author LIAS Laboratory
 * @see Document
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // =========================================================================
    // 1. RECHERCHE PAR ÉVÉNEMENT
    // =========================================================================

    /**
     * Récupère tous les documents rattachés à un événement spécifique.
     *
     * <p>Utilise la navigation automatique de Spring Data JPA à travers
     * la relation {@code evenement} pour filtrer par l'identifiant de
     * l'{@link com.lias.gestion_laboratoire.models.Evenement}.</p>
     *
     * <p><strong>Cas d'usage :</strong> afficher la liste des documents
     * archivés (programme, attestations, rapports…) sur la page de détail
     * d'un événement scientifique dans le portail du laboratoire LIAS.</p>
     *
     * @param evenementId l'identifiant de l'événement concerné
     * @return liste des documents liés à l'événement
     */
    List<Document> findByEvenementId(Long evenementId);

    List<Document> findByEstArchiveFalseOrderByDateUploadDesc();

    List<Document> findByEstArchiveTrueOrderByDateArchivageDesc();

    List<Document> findByEstArchiveFalseAndNomFichierContainingIgnoreCase(String nomFichier);

    Optional<Document> findByIdAndEstArchiveFalse(Long id);

    // =========================================================================
    // 2. RECHERCHE PAR TYPE DE DOCUMENT
    // =========================================================================

    /**
     * Récupère tous les documents d'un type donné.
     *
     * <p><strong>Cas d'usage :</strong> filtrer les documents archivés
     * par catégorie (DEMANDE_FINANCEMENT, PROGRAMME, ATTESTATION, RAPPORT,
     * DOCUMENT_ADMINISTRATIF) dans l'interface de gestion documentaire
     * du laboratoire LIAS.</p>
     *
     * @param type le type de document recherché
     * @return liste des documents correspondant au type demandé
     * @see TypeDocument
     */
    List<Document> findByTypeDocument(TypeDocument type);
}
