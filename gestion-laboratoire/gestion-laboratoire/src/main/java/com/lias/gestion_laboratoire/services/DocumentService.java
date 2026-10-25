package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.models.Document;
import com.lias.gestion_laboratoire.models.Evenement;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.RoleUtilisateur;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.models.enums.TypeDocument;
import com.lias.gestion_laboratoire.authorization.AuthorizationService;
import com.lias.gestion_laboratoire.dto.DocumentResponse;
import com.lias.gestion_laboratoire.repositories.DocumentRepository;
import com.lias.gestion_laboratoire.repositories.EvenementRepository;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Service métier pour la gestion de l'archivage documentaire
 * du laboratoire de recherche LIAS.
 *
 * <p>Cette classe centralise toute la logique métier relative aux documents :
 * upload physique sur le système de fichiers, persistance des métadonnées
 * en base de données, consultation, et chargement pour téléchargement.</p>
 *
 * <p><strong>Stratégie de stockage :</strong> Les fichiers physiques sont
 * stockés dans le répertoire {@code uploads/documents/} à la racine du
 * projet. Un nom de fichier unique est généré via {@link UUID} pour
 * éviter tout conflit de nommage.</p>
 *
 * <p><strong>Gestion des erreurs :</strong> Les exceptions d'entrées/sorties
 * ({@link IOException}) sont transformées en {@link RuntimeException} métier
 * avec un message explicatif pour remonter proprement dans la couche
 * contrôleur et le gestionnaire d'exceptions global.</p>
 *
 * @author LIAS Laboratory
 * @see Document
 * @see DocumentRepository
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DocumentService {

    /** Repository des documents, injecté via le constructeur généré par Lombok. */
    private final DocumentRepository documentRepository;
    private final EvenementRepository evenementRepository;
    private final MembreRepository membreRepository;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;

    /**
     * Chemin du répertoire de stockage physique des fichiers uploadés.
     *
     * <p>Ce répertoire est créé automatiquement au démarrage de l'application
     * par la méthode {@link #init()} s'il n'existe pas encore.</p>
     */
    private static final String UPLOAD_DIR = "uploads/documents/";
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".rtf", ".png", ".jpg", ".jpeg");

    // =========================================================================
    // INITIALISATION — CRÉATION DU DOSSIER DE STOCKAGE
    // =========================================================================

    /**
     * Crée le répertoire de stockage physique des documents au démarrage
     * de l'application s'il n'existe pas encore.
     *
     * <p>Cette méthode est appelée automatiquement par le conteneur Spring
     * après l'injection des dépendances grâce à l'annotation
     * {@link PostConstruct}.</p>
     *
     * @throws RuntimeException si la création du répertoire échoue
     *                          (permissions insuffisantes, disque plein…)
     */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            throw new RuntimeException(
                    "Impossible de créer le dossier de stockage des documents : " + UPLOAD_DIR, e);
        }
    }

    // =========================================================================
    // UPLOAD DE DOCUMENT
    // =========================================================================

    /**
     * Uploade un fichier physique et persiste ses métadonnées en base de données.
     *
     * <p><strong>Processus complet :</strong></p>
     * <ol>
     *   <li>Vérifie l'existence de l'événement cible via son identifiant.</li>
     *   <li>Génère un nom de fichier unique en combinant un {@link UUID}
     *       aléatoire avec l'extension d'origine du fichier uploadé.</li>
     *   <li>Copie physiquement le fichier dans le répertoire
     *       {@value #UPLOAD_DIR} du serveur.</li>
     *   <li>Crée une instance de l'entité {@link Document} avec toutes
     *       les métadonnées (nom original, chemin, type MIME, taille,
     *       type de document, événement associé).</li>
     *   <li>Sauvegarde l'entité en base de données et la retourne.</li>
     * </ol>
     *
     * @param fichier      le fichier uploadé via le formulaire multipart
     * @param evenementId  l'identifiant de l'événement auquel rattacher le document
     * @param type         le type de document (PROGRAMME, ATTESTATION, etc.)
     * @return l'entité {@link Document} sauvegardée avec son identifiant généré
     * @throws EntityNotFoundException si aucun événement ne correspond à l'ID fourni
     * @throws RuntimeException        si une erreur d'entrée/sortie survient
     *                                 lors de la copie physique du fichier
     */
    public DocumentResponse uploaderDocument(MultipartFile fichier, Long evenementId, TypeDocument type) {
        Membre deposant = currentMember();
        authorizationService.verifierDepotDocument(deposant);
        validerFichier(fichier, type);
        if (evenementId == null) {
            if (type != TypeDocument.DOCUMENT_ADMINISTRATIF
                    && type != TypeDocument.PV
                    && type != TypeDocument.CONVENTION) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Un événement est obligatoire pour ce type de document.");
            }
        }
        Evenement evenement = evenementId == null ? null : evenementRepository.findById(evenementId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucun événement trouvé avec l'ID : " + evenementId));

        String nomOriginal = Paths.get(fichier.getOriginalFilename()).getFileName().toString();
        String extension = nomOriginal.substring(nomOriginal.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        Path dossierStockage = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
        Path cheminCible = dossierStockage.resolve(UUID.randomUUID() + extension).normalize();
        if (!cheminCible.startsWith(dossierStockage)) {
            throw new IllegalStateException("Chemin de stockage documentaire invalide.");
        }
        try {
            try (InputStream inputStream = fichier.getInputStream()) {
                Files.copy(inputStream, cheminCible, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la sauvegarde du document.", e);
        }

        try {
            Document document = new Document();
            document.setNomFichier(nomOriginal);
            document.setCheminStockage(cheminCible.toString());
            document.setContentType(fichier.getContentType());
            document.setTaille(fichier.getSize());
            document.setTypeDocument(type);
            document.setEvenement(evenement);
            document.setDeposePar(deposant);
            document = documentRepository.save(document);
            DocumentResponse response = responseFor(document, deposant);
            notifierAjoutDocument(evenement, deposant, nomOriginal);
            return response;
        } catch (RuntimeException e) {
            try {
                Files.deleteIfExists(cheminCible);
            } catch (IOException cleanupException) {
                e.addSuppressed(cleanupException);
            }
            throw e;
        }
    }

    // =========================================================================
    // CONSULTATION — OBTENIR UN DOCUMENT PAR ID
    // =========================================================================

    /**
     * Récupère un document par son identifiant.
     *
     * @param id l'identifiant du document recherché
     * @return l'entité {@link Document} correspondante
     * @throws EntityNotFoundException si aucun document ne correspond à l'ID fourni
     */
    @Transactional(readOnly = true)
    public Document obtenirDocument(Long id) {
        Membre membre = currentMember();
        authorizationService.verifierAccesDocuments(membre);
        return documentRepository.findByIdAndEstArchiveFalse(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucun document actif trouvé avec l'ID : " + id));
    }

    @Transactional(readOnly = true)
    public DocumentResponse obtenirDocumentResponse(Long id) {
        Membre membre = currentMember();
        authorizationService.verifierAccesDocuments(membre);
        Document document = documentRepository.findByIdAndEstArchiveFalse(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucun document actif trouvé avec l'ID : " + id));
        return responseFor(document, membre);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listerDocuments(TypeDocument type, Long evenementId, Integer annee) {
        Membre membre = currentMember();
        authorizationService.verifierAccesDocuments(membre);
        return documentRepository.findByEstArchiveFalseOrderByDateUploadDesc().stream()
                .filter(document -> type == null || document.getTypeDocument() == type)
                .filter(document -> evenementId == null || (document.getEvenement() != null
                        && evenementId.equals(document.getEvenement().getId())))
                .filter(document -> annee == null || (document.getDateUpload() != null
                        && document.getDateUpload().getYear() == annee))
                .map(document -> responseFor(document, membre))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listerArchives() {
        Membre membre = currentMember();
        authorizationService.verifierAccesDocuments(membre);
        if (!estSupervision(membre)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "L'accès à l'archive documentaire est réservé aux administrateurs et à la direction.");
        }
        return documentRepository.findByEstArchiveTrueOrderByDateArchivageDesc().stream()
                .map(document -> DocumentResponse.from(document, false))
                .toList();
    }

    public DocumentResponse archiver(Long id) {
        Document document = obtenirDocument(id);
        Membre membre = currentMember();
        authorizationService.verifierGestionDocument(membre, document);
        document.setEstArchive(true);
        document.setDateArchivage(LocalDateTime.now());
        document.setArchivePar(membre);
        return responseFor(documentRepository.save(document), membre);
    }

    public DocumentResponse restaurer(Long id) {
        Membre membre = currentMember();
        authorizationService.verifierAccesDocuments(membre);
        if (!estSupervision(membre)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seule la direction peut restaurer un document archivé.");
        }
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Aucun document trouvé avec l'ID : " + id));
        document.setEstArchive(false);
        return responseFor(documentRepository.save(document), membre);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> rechercherDocumentsPourRechercheGlobale(String keyword, Membre membre) {
        if (keyword == null || keyword.trim().length() < 2) {
            return List.of();
        }
        return documentRepository.findByEstArchiveFalseAndNomFichierContainingIgnoreCase(keyword.trim()).stream()
                .map(document -> responseFor(document, membre))
                .toList();
    }

    // =========================================================================
    // CONSULTATION — DOCUMENTS PAR ÉVÉNEMENT
    // =========================================================================

    /**
     * Récupère tous les documents rattachés à un événement spécifique.
     *
     * @param evenementId l'identifiant de l'événement
     * @return liste des documents liés à cet événement
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> obtenirDocumentsParEvenement(Long evenementId) {
        Membre membre = currentMember();
        authorizationService.verifierAccesDocuments(membre);
        return documentRepository.findByEvenementId(evenementId).stream()
                .filter(document -> !document.isEstArchive())
                .map(document -> responseFor(document, membre))
                .toList();
    }

    // =========================================================================
    // CONSULTATION — DOCUMENTS PAR TYPE
    // =========================================================================

    /**
     * Récupère tous les documents d'un type donné.
     *
     * @param type le type de document recherché
     * @return liste des documents correspondant au type demandé
     * @see TypeDocument
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> obtenirDocumentsParType(TypeDocument type) {
        Membre membre = currentMember();
        authorizationService.verifierAccesDocuments(membre);
        return documentRepository.findByTypeDocument(type).stream()
                .filter(document -> !document.isEstArchive())
                .map(document -> responseFor(document, membre))
                .toList();
    }

    // =========================================================================
    // CHARGEMENT DU FICHIER PHYSIQUE POUR TÉLÉCHARGEMENT
    // =========================================================================

    /**
     * Charge le fichier physique associé à un document pour téléchargement.
     *
     * <p>Cette méthode récupère le document en base de données, puis utilise
     * {@link UrlResource} pour charger le fichier depuis le système de fichiers
     * du serveur en se basant sur le champ {@code cheminStockage}.</p>
     *
     * <p><strong>Cas d'usage :</strong> endpoint REST de téléchargement
     * renvoyant le fichier physique au client via un {@code ResponseEntity<Resource>}.</p>
     *
     * @param documentId l'identifiant du document dont on souhaite charger le fichier
     * @return une {@link Resource} représentant le fichier physique, prête
     *         à être transmise dans une réponse HTTP
     * @throws EntityNotFoundException si aucun document ne correspond à l'ID fourni
     * @throws RuntimeException        si le fichier physique est introuvable sur
     *                                 le disque ou si l'URL est malformée
     */
    @Transactional(readOnly = true)
    public Resource chargerFichierPhysique(Long documentId) {

        // 1. Récupérer le document en base
        Document document = obtenirDocument(documentId);

        // 2. Charger le fichier physique depuis le disque
        try {
            Path cheminFichier = Paths.get(document.getCheminStockage()).normalize();
            Resource resource = new UrlResource(cheminFichier.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException(
                        "Le fichier est introuvable ou illisible : " + document.getCheminStockage());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(
                    "Erreur lors du chargement du fichier : " + document.getCheminStockage(), e);
        }
    }

    private void validerFichier(MultipartFile fichier, TypeDocument type) {
        if (fichier == null || fichier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier est obligatoire.");
        }
        if (fichier.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La taille maximale autorisée est de 5 Mo.");
        }
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le type de document est obligatoire.");
        }
        String filename = fichier.getOriginalFilename();
        if (filename == null || filename.isBlank() || filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom du fichier est invalide.");
        }
        int dot = filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Format de fichier non autorisé. Formats acceptés : PDF, Office, TXT, RTF, PNG et JPEG.");
        }
    }

    private Membre currentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Membre membre)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Un compte membre authentifié est requis pour accéder aux documents.");
        }
        return membre;
    }

    private boolean estSupervision(Membre membre) {
        return membre.getRole() == Role.ADMIN || membre.getRole() == Role.DIRECTEUR
                || membre.getRole() == Role.VICE_DIRECTEUR
                || membre.getRoleSysteme() == RoleUtilisateur.ADMIN
                || membre.getRoleSysteme() == RoleUtilisateur.DIRECTEUR;
    }

    private void notifierAjoutDocument(Evenement evenement, Membre deposant, String nomFichier) {
        Set<Long> destinataires = new LinkedHashSet<>();
        if (evenement != null) {
            if (evenement.getOrganisateurs() != null) {
                evenement.getOrganisateurs().forEach(membre -> destinataires.add(membre.getId()));
            }
            if (evenement.getParticipants() != null) {
                evenement.getParticipants().forEach(membre -> destinataires.add(membre.getId()));
            }
        } else {
            membreRepository.findByCompteActif(true).stream()
                    .filter(membre -> membre.isCompteActif() && !membre.isCompteGele())
                    .filter(membre -> membre.getStatut() != Statut.DOCTORANT
                            && membre.getStatut() != Statut.RETRAITE && membre.getStatut() != Statut.ANCIEN)
                    .filter(membre -> membre.getRole() == Role.ADMIN || membre.getRole() == Role.DIRECTEUR
                            || membre.getRole() == Role.VICE_DIRECTEUR
                            || membre.getRoleSysteme() == RoleUtilisateur.ADMIN
                            || membre.getRoleSysteme() == RoleUtilisateur.DIRECTEUR)
                    .forEach(membre -> destinataires.add(membre.getId()));
        }
        destinataires.remove(deposant.getId());
        if (destinataires.isEmpty()) {
            destinataires.add(deposant.getId());
        }
        String contexte = evenement == null ? "" : " pour l'événement « " + evenement.getTitre() + " »";
        destinataires.forEach(id -> notificationService.creerNotification(
                id, "Nouveau document", "Le document « " + nomFichier + " » a été ajouté" + contexte + ".",
                com.lias.gestion_laboratoire.models.enums.TypeNotification.DOCUMENT,
                "DOCUMENT",
                null));
    }

    private DocumentResponse responseFor(Document document, Membre membre) {
        return DocumentResponse.from(document, authorizationService.peutGererDocument(membre, document));
    }
}
