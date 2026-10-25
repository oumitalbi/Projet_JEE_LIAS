package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.Statut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository Spring Data JPA pour l'entité {@link Membre}.
 *
 * <p>Ce repository fournit l'ensemble des opérations CRUD standard via
 * {@link JpaRepository} ainsi que des méthodes de recherche personnalisées
 * adaptées aux règles métier du laboratoire LIAS.</p>
 *
 * <p><strong>Règle métier de non-suppression :</strong> Les membres ne sont
 * jamais supprimés physiquement. Toutes les requêtes orientées « affichage »
 * filtrent par {@code compteActif = true} afin de ne présenter que les
 * membres dont le compte est actif.</p>
 *
 * @author LIAS Laboratory
 * @see Membre
 */
@Repository
public interface MembreRepository extends JpaRepository<Membre, Long> {

    // =========================================================================
    // 1. GESTION DES ACTIFS (SOFT DELETE)
    // =========================================================================

    /**
     * Récupère la liste de tous les membres dont le compte est actif.
     *
     * <p>Cette méthode est le point d'entrée principal pour l'affichage
     * des membres : elle exclut automatiquement les membres désactivés
     * (soft-deleted) conformément à la règle de non-suppression du
     * laboratoire LIAS.</p>
     *
     * @param compteActif {@code true} pour les membres actifs,
     *                    {@code false} pour les membres désactivés
     * @return liste des membres correspondant à l'état demandé
     */
    List<Membre> findByCompteActif(boolean compteActif);

    // =========================================================================
    // 2. RECHERCHE GLOBALE (NOM / PRÉNOM)
    // =========================================================================

    /**
     * Recherche des membres actifs par nom <strong>ou</strong> prénom.
     *
     * <p>La recherche est insensible à la casse et partielle (contient) :
     * saisir {@code "ali"} remontera par exemple « Aliane », « Khalid », etc.
     * Seuls les membres dont le compte est actif sont retournés.</p>
     *
     * <p><strong>Cas d'usage :</strong> barre de recherche dans l'interface
     * de gestion des membres du laboratoire LIAS.</p>
     *
     * @param nom    fragment du nom à rechercher (insensible à la casse)
     * @param prenom fragment du prénom à rechercher (insensible à la casse)
     * @param compteActif {@code true} pour ne retourner que les comptes actifs
     * @return liste des membres correspondant aux critères de recherche
     */
    @Query("SELECT m FROM Membre m WHERE (LOWER(m.nom) LIKE LOWER(CONCAT('%', :nom, '%')) OR LOWER(m.prenom) LIKE LOWER(CONCAT('%', :prenom, '%'))) AND m.compteActif = :compteActif")
    List<Membre> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCaseAndCompteActif(
            @Param("nom") String nom, @Param("prenom") String prenom, @Param("compteActif") boolean compteActif);

    // =========================================================================
    // 3. FILTRAGE PAR ÉQUIPE DE RECHERCHE
    // =========================================================================

    /**
     * Récupère tous les membres actifs appartenant à une équipe de recherche
     * spécifique du laboratoire LIAS.
     *
     * <p><strong>Cas d'usage :</strong> afficher la composition d'une équipe
     * (ex : ISDIAC, SIMA, ILIAS, SDTIC) sur la page de présentation du
     * laboratoire ou dans le tableau de bord de gestion.</p>
     *
     * @param equipeLaboratoire l'équipe de recherche ciblée
     * @param compteActif       {@code true} pour ne retourner que les comptes actifs
     * @return liste des membres actifs rattachés à l'équipe spécifiée
     * @see EquipeLaboratoire
     */
    List<Membre> findByEquipeLaboratoireAndCompteActif(
            EquipeLaboratoire equipeLaboratoire, boolean compteActif);

    // =========================================================================
    // 4. FILTRAGE PAR RÔLE FONCTIONNEL
    // =========================================================================

    /**
     * Récupère les membres actifs ayant un rôle fonctionnel spécifique au
     * sein du laboratoire LIAS.
     *
     * <p><strong>Cas d'usage :</strong> identifier rapidement le directeur
     * du laboratoire, les chefs d'équipe, ou filtrer les administrateurs
     * pour la gestion des droits d'accès.</p>
     *
     * @param role        le rôle fonctionnel recherché (ex : DIRECTEUR, CHEF_EQUIPE)
     * @param compteActif {@code true} pour ne retourner que les comptes actifs
     * @return liste des membres actifs correspondant au rôle demandé
     * @see Role
     */
    List<Membre> findByRoleAndCompteActif(Role role, boolean compteActif);

    // =========================================================================
    // 5. FILTRAGE PAR STATUT PROFESSIONNEL
    // =========================================================================

    /**
     * Récupère les membres selon leur statut professionnel au sein du
     * laboratoire LIAS.
     *
     * <p><strong>Cas d'usage :</strong> lister tous les doctorants en cours
     * de thèse, afficher les membres permanents pour les rapports HCERES,
     * ou consulter les anciens membres à des fins d'archivage.</p>
     *
     * @param statut      le statut professionnel recherché (ex : DOCTORANT, PERMANENT)
     * @param compteActif {@code true} pour ne retourner que les comptes actifs
     * @return liste des membres correspondant au statut demandé
     * @see Statut
     */
    List<Membre> findByStatutAndCompteActif(Statut statut, boolean compteActif);

    // =========================================================================
    // 6. VÉRIFICATION D'UNICITÉ DE L'EMAIL ET AUTHENTIFICATION (SÉCURITÉ)
    // =========================================================================

    /**
     * Vérifie si un email existe déjà dans la base de données du laboratoire.
     *
     * <p>Cette méthode est utilisée <strong>avant l'inscription</strong> ou
     * la mise à jour d'un membre afin d'empêcher les doublons d'adresses
     * e-mail, conformément à la contrainte d'unicité définie sur le champ
     * {@code email} de l'entité {@link Membre}.</p>
     *
     * <p><strong>Note :</strong> la vérification porte sur <em>tous</em> les
     * membres (actifs et désactivés) pour garantir une unicité absolue.</p>
     *
     * @param email l'adresse e-mail à vérifier
     * @return {@code true} si l'email est déjà utilisé, {@code false} sinon
     */
    boolean existsByEmail(String email);

    /**
     * Récupère un membre par son adresse email.
     * Cette méthode sera le cœur du futur système de Login.
     *
     * @param email l'adresse e-mail du membre
     * @return un Optional contenant le membre s'il est trouvé
     */
    java.util.Optional<Membre> findByEmail(String email);

    java.util.Optional<Membre> findByEmailIgnoreCase(String email);
    // =========================================================================
    // 7. RECHERCHE GLOBALE (MODULE 18)
    // =========================================================================

    /**
     * Recherche globale par mot-clé à travers les champs texte principaux.
     */
    List<Membre> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCaseOrBiographieContainingIgnoreCase(String nom, String prenom, String biographie);

    // =========================================================================
    // 8. FILTRAGE PAR DATE D'AFFILIATION (RAPPORT ANNUEL)
    // =========================================================================

    /**
     * Récupère les membres dont la date d'affiliation est comprise entre
     * deux dates — utilisé pour compter les nouvelles adhésions d'une année.
     *
     * <p><strong>Cas d'usage :</strong> générer la section « nouveaux membres »
     * du rapport d'activité annuel du laboratoire LIAS.</p>
     *
     * @param debut date de début de la période (1er janvier de l'année)
     * @param fin   date de fin de la période (31 décembre de l'année)
     * @return liste des membres affiliés dans la période spécifiée
     */
    List<Membre> findByDateAffiliationBetween(LocalDate debut, LocalDate fin);
}
