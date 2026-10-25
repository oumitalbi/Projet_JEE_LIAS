package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.Publication;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.TypePublication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité {@link Publication}.
 *
 * <p>Ce repository fournit l'ensemble des opérations CRUD standard via
 * {@link JpaRepository} ainsi que des méthodes de recherche personnalisées
 * adaptées aux règles métier du laboratoire LIAS.</p>
 *
 * <p><strong>Règle métier de non-suppression :</strong> Les publications ne
 * sont jamais supprimées physiquement. Toutes les requêtes orientées
 * « affichage » filtrent par {@code estArchive = false} afin de ne
 * présenter que les publications actives.</p>
 *
 * @author LIAS Laboratory
 * @see Publication
 */
@Repository
public interface PublicationRepository extends JpaRepository<Publication, Long> {

        Optional<Publication> findByTitre(String titre);

    // =========================================================================
    // 1. PUBLICATIONS NON ARCHIVÉES (SOFT DELETE)
    // =========================================================================

    /**
     * Récupère toutes les publications non archivées.
     *
     * <p>Cette méthode est le point d'entrée principal pour l'affichage
     * des publications : elle exclut automatiquement les publications
     * archivées (soft-deleted) conformément à la règle de non-suppression
     * du laboratoire LIAS.</p>
     *
     * @param estArchive {@code false} pour les publications actives,
     *                   {@code true} pour les publications archivées
     * @return liste des publications correspondant à l'état demandé
     */
    List<Publication> findByEstArchive(boolean estArchive);

    // =========================================================================
    // 2. FILTRAGE PAR TYPE DE PUBLICATION
    // =========================================================================

    /**
     * Récupère les publications non archivées d'un type spécifique.
     *
     * <p><strong>Cas d'usage :</strong> filtrer les publications par catégorie
     * (article de journal, conférence, chapitre de livre, thèse, brevet)
     * sur la page de consultation du laboratoire LIAS.</p>
     *
     * @param type       le type de publication recherché
     * @param estArchive {@code false} pour ne retourner que les publications actives
     * @return liste des publications non archivées du type demandé
     * @see TypePublication
     */
    List<Publication> findByTypeAndEstArchive(TypePublication type, boolean estArchive);

    // =========================================================================
    // 3. FILTRAGE PAR AUTEUR (MEMBRE)
    // =========================================================================

    /**
     * Récupère les publications non archivées d'un auteur spécifique
     * identifié par son identifiant de membre.
     *
     * <p>Utilise la navigation automatique de Spring Data JPA à travers
     * la relation {@code auteurs} pour filtrer par l'identifiant du
     * {@link com.lias.gestion_laboratoire.models.Membre}.</p>
     *
     * <p><strong>Cas d'usage :</strong> afficher la liste des publications
     * d'un membre sur sa page de profil dans le laboratoire LIAS.</p>
     *
     * @param membreId l'identifiant du membre auteur
     * @return liste des publications non archivées de l'auteur spécifié
     */
    List<Publication> findByAuteurs_IdAndEstArchiveFalse(Long membreId);

    // =========================================================================
    // 4. FILTRAGE PAR ANNÉE DE PUBLICATION
    // =========================================================================

    /**
     * Récupère les publications non archivées publiées entre deux dates.
     *
     * <p>Pour rechercher les publications d'une année précise, passer
     * le 1er janvier comme {@code debut} et le 31 décembre comme
     * {@code fin} de l'année ciblée.</p>
     *
     * <p><strong>Cas d'usage :</strong> générer un rapport annuel des
     * publications du laboratoire LIAS pour l'évaluation HCERES.</p>
     *
     * @param debut      date de début de la période (incluse)
     * @param fin        date de fin de la période (incluse)
     * @param estArchive {@code false} pour ne retourner que les publications actives
     * @return liste des publications non archivées dans la période spécifiée
     */
    List<Publication> findByDatePublicationBetweenAndEstArchive(
            LocalDate debut, LocalDate fin, boolean estArchive);

    // =========================================================================
    // 5. FILTRAGE PAR ÉQUIPE DE RECHERCHE
    // =========================================================================

    /**
     * Récupère les publications non archivées dont au moins un auteur
     * appartient à l'équipe de recherche spécifiée.
     *
     * <p>Utilise la navigation automatique de Spring Data JPA à travers
     * la relation {@code auteurs} puis le champ {@code equipeLaboratoire}
     * du {@link com.lias.gestion_laboratoire.models.Membre}.</p>
     *
     * <p><strong>Cas d'usage :</strong> afficher les publications produites
     * par une équipe spécifique (ISDIAC, SIMA, ILIAS, SDTIC) dans le
     * tableau de bord du laboratoire LIAS.</p>
     *
     * @param equipe l'équipe de recherche ciblée
     * @return liste des publications non archivées de l'équipe spécifiée
     * @see EquipeLaboratoire
     */
    List<Publication> findByAuteurs_EquipeLaboratoireAndEstArchiveFalse(
            EquipeLaboratoire equipe);
    // =========================================================================
    // 6. RECHERCHE GLOBALE (MODULE 18)
    // =========================================================================

    /**
     * Recherche globale par mot-clé à travers les champs texte principaux.
     */
    List<Publication> findByTitreContainingIgnoreCaseOrResumeContainingIgnoreCase(String titre, String resume);
}
