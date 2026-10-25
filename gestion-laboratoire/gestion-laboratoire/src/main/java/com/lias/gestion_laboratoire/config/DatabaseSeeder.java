package com.lias.gestion_laboratoire.config;

import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.Mandat;
import com.lias.gestion_laboratoire.models.Evenement;
import com.lias.gestion_laboratoire.models.Publication;
import com.lias.gestion_laboratoire.models.Document;
import com.lias.gestion_laboratoire.models.Reunion;
import com.lias.gestion_laboratoire.models.ReunionProcesVerbal;
import com.lias.gestion_laboratoire.models.Convention;
import com.lias.gestion_laboratoire.models.Partenaire;
import com.lias.gestion_laboratoire.models.Materiel;
import com.lias.gestion_laboratoire.models.DemandeMateriel;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.RoleUtilisateur;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.models.enums.TypeEvenement;
import com.lias.gestion_laboratoire.models.enums.StatutReunion;
import com.lias.gestion_laboratoire.models.enums.TypeDocument;
import com.lias.gestion_laboratoire.models.enums.TypePartenaire;
import com.lias.gestion_laboratoire.models.enums.EtatMateriel;
import com.lias.gestion_laboratoire.models.enums.StatutDemande;
import com.lias.gestion_laboratoire.repositories.EvenementRepository;
import com.lias.gestion_laboratoire.repositories.DocumentRepository;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import com.lias.gestion_laboratoire.repositories.MandatRepository;
import com.lias.gestion_laboratoire.repositories.PublicationRepository;
import com.lias.gestion_laboratoire.repositories.ReunionRepository;
import com.lias.gestion_laboratoire.repositories.ReunionProcesVerbalRepository;
import com.lias.gestion_laboratoire.repositories.PartenaireRepository;
import com.lias.gestion_laboratoire.repositories.ConventionRepository;
import com.lias.gestion_laboratoire.repositories.MaterielRepository;
import com.lias.gestion_laboratoire.repositories.DemandeMaterielRepository;
import com.lias.gestion_laboratoire.repositories.NotificationRepository;
import com.lias.gestion_laboratoire.repositories.MessageRepository;
import com.lias.gestion_laboratoire.repositories.MessageLectureRepository;
import com.lias.gestion_laboratoire.models.Notification;
import com.lias.gestion_laboratoire.models.Message;
import com.lias.gestion_laboratoire.models.MessageLecture;
import com.lias.gestion_laboratoire.models.enums.TypeNotification;
import com.lias.gestion_laboratoire.models.enums.TypeMessage;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);
    private static final String ADMIN_INITIAL_PASSWORD = "password123";

    private final MembreRepository membreRepository;
    private final MandatRepository mandatRepository;
    private final PublicationRepository publicationRepository;
    private final EvenementRepository evenementRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReunionRepository reunionRepository;
    private final DocumentRepository documentRepository;
    private final ReunionProcesVerbalRepository reunionProcesVerbalRepository;
    private final PartenaireRepository partenaireRepository;
    private final ConventionRepository conventionRepository;
    private final MaterielRepository materielRepository;
    private final DemandeMaterielRepository demandeMaterielRepository;
    private final NotificationRepository notificationRepository;
    private final MessageRepository messageRepository;
    private final MessageLectureRepository messageLectureRepository;

    public DatabaseSeeder(MembreRepository membreRepository,
                          MandatRepository mandatRepository,
                          PublicationRepository publicationRepository,
                          EvenementRepository evenementRepository,
                          ReunionRepository reunionRepository,
                          DocumentRepository documentRepository,
                          ReunionProcesVerbalRepository reunionProcesVerbalRepository,
                          PartenaireRepository partenaireRepository,
                          ConventionRepository conventionRepository,
                          MaterielRepository materielRepository,
                          DemandeMaterielRepository demandeMaterielRepository,
                          NotificationRepository notificationRepository,
                          MessageRepository messageRepository,
                          MessageLectureRepository messageLectureRepository,
                          PasswordEncoder passwordEncoder) {
        this.membreRepository = membreRepository;
        this.mandatRepository = mandatRepository;
        this.publicationRepository = publicationRepository;
        this.evenementRepository = evenementRepository;
        this.reunionRepository = reunionRepository;
        this.documentRepository = documentRepository;
        this.reunionProcesVerbalRepository = reunionProcesVerbalRepository;
        this.partenaireRepository = partenaireRepository;
        this.conventionRepository = conventionRepository;
        this.materielRepository = materielRepository;
        this.demandeMaterielRepository = demandeMaterielRepository;
        this.notificationRepository = notificationRepository;
        this.messageRepository = messageRepository;
        this.messageLectureRepository = messageLectureRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // =====================================================================
        // MISSION 1 : Archivage logique des comptes Directrice en doublon
        // =====================================================================
        List<Membre> allMembres = membreRepository.findAll();
        List<Membre> directrices = allMembres.stream()
                .filter(m -> "Benabbou".equalsIgnoreCase(m.getNom())
                          && "Faouzia".equalsIgnoreCase(m.getPrenom()))
                .toList();

        if (directrices.size() > 1) {
            log.info("{} comptes 'Faouzia Benabbou' détectés; archivage logique des doublons.",
                    directrices.size());

            Membre comptePrincipal = directrices.stream()
                    .filter(membre -> "f.benabbolias@gmail.com".equalsIgnoreCase(membre.getEmail()))
                    .findFirst()
                    .orElseGet(() -> directrices.stream()
                            .min((first, second) -> Long.compare(first.getId(), second.getId()))
                            .orElseThrow());
            List<Membre> comptesArchives = new ArrayList<>(directrices);
            comptesArchives.remove(comptePrincipal);
            for (Membre compteArchive : comptesArchives) {
                compteArchive.setCompteActif(false);
                compteArchive.setCompteGele(true);
                compteArchive.setStatut(Statut.ANCIEN);
                membreRepository.save(compteArchive);
                log.info("Compte doublon archivé logiquement (ID {}).", compteArchive.getId());
            }
            comptePrincipal.setRole(Role.DIRECTEUR);
            comptePrincipal.setRoleSysteme(RoleUtilisateur.DIRECTEUR);
            comptePrincipal.setEquipeLaboratoire(EquipeLaboratoire.AUCUNE);
            comptePrincipal.setCompteActif(true);
            comptePrincipal.setCompteGele(false);
            comptePrincipal.setStatut(Statut.PERMANENT);
            comptePrincipal.setMotDePasse(passwordEncoder.encode("admin123"));
            membreRepository.save(comptePrincipal);
            log.info("Compte directeur principal conservé (ID {}).", comptePrincipal.getId());

        } else if (directrices.size() == 1) {
            Membre directrice = directrices.get(0);
            directrice.setEmail("f.benabbolias@gmail.com");
            directrice.setMotDePasse(passwordEncoder.encode("admin123"));
            directrice.setRole(Role.DIRECTEUR);
            directrice.setRoleSysteme(RoleUtilisateur.DIRECTEUR);
            directrice.setEquipeLaboratoire(EquipeLaboratoire.AUCUNE);
            directrice.setCompteActif(true);
            directrice.setCompteGele(false);
            membreRepository.save(directrice);
            log.info("Compte Directrice mis à jour (ID {}).", directrice.getId());
        }

        // =====================================================================
        // MISSION 2 : Seeder les autres membres (idempotent)
        // =====================================================================
        String pw = passwordEncoder.encode("password123");
        seedMembre("Belangour",  "Abdessamad", "a.belangour@lias.ma",  Role.VICE_DIRECTEUR, EquipeLaboratoire.AUCUNE, RoleUtilisateur.VICE_DIRECTEUR, pw);
        seedMembre("Sael",       "Nawal",       "n.sael@lias.ma",       Role.CHEF_EQUIPE,    EquipeLaboratoire.ISDIAC,    RoleUtilisateur.MEMBRE_PERMANENT, pw);
        seedMembre("Ait Daoud",  "Mohammed",    "m.aitdaoud@lias.ma",   Role.CHEF_EQUIPE,    EquipeLaboratoire.SIMA,      RoleUtilisateur.MEMBRE_PERMANENT, pw);
        seedMembre("Ouahabi",    "Sara",        "s.ouahabi@lias.ma",    Role.CHEF_EQUIPE,    EquipeLaboratoire.SDTIC,     RoleUtilisateur.MEMBRE_PERMANENT, pw);
        seedMembre("Ettaoufik",  "Abdelaziz",   "a.ettaoufik@lias.ma",  Role.CHEF_EQUIPE,    EquipeLaboratoire.ILIAS,     RoleUtilisateur.MEMBRE_PERMANENT, pw);

        configurerRoleViceDirecteur();
        seedCompteAdministrateur();

        seedMandats();

        // =====================================================================
        // MISSION 3 : Événements de démonstration idempotents
        // =====================================================================
        seedEvenements();

        // =====================================================================
        // MISSION 4 : Seeder les publications LIAS existantes
        // =====================================================================
        seedPublications();

        // =====================================================================
        // MISSION 5 : Réunions LIAS idempotentes; aucun enregistrement supprimé
        // =====================================================================
        seedReunions();

        // =====================================================================
        // MISSION 6 : Partenaires et conventions de démonstration idempotents
        // =====================================================================
        seedConventions();

        // =====================================================================
        // MISSION 7 : Inventaire logistique LIAS idempotent
        // =====================================================================
        seedLogistique();

        log.info("Seeding terminé. Base de données prête.");
    }

    private void seedEvenements() {
        Membre benabbou = membreRepository.findByEmail("f.benabbolias@gmail.com").orElse(null);
        Membre belangour = membreRepository.findByEmail("a.belangour@lias.ma").orElse(null);
        Membre sael = membreRepository.findByEmail("n.sael@lias.ma").orElse(null);
        Membre aitDaoud = membreRepository.findByEmail("m.aitdaoud@lias.ma").orElse(null);
        Membre ouahabi = membreRepository.findByEmail("s.ouahabi@lias.ma").orElse(null);
        Membre ettaoufik = membreRepository.findByEmail("a.ettaoufik@lias.ma").orElse(null);

        List<Membre> organisateursDisponibles = java.util.stream.Stream
                .of(benabbou, belangour, sael, aitDaoud, ouahabi, ettaoufik)
                .filter(java.util.Objects::nonNull).toList();
        if (organisateursDisponibles.isEmpty()) {
            log.warn("Aucun organisateur disponible; les événements de démonstration n'ont pas été ajoutés.");
            return;
        }

        int index = 0;
        for (Evenement evenement : evenementRepository.findAll()) {
            if (evenement.getOrganisateurs() == null || evenement.getOrganisateurs().isEmpty()) {
                Membre organisateur = organisateursDisponibles.get(index++ % organisateursDisponibles.size());
                evenement.getOrganisateurs().add(organisateur);
                if (evenement.getCreePar() == null) {
                    evenement.setCreePar(organisateur);
                }
                evenementRepository.save(evenement);
                log.info("Organisateur affecté à l'ancien événement ID {}.", evenement.getId());
            }
            if (evenement.getTitre() == null || evenement.getTitre().isBlank()
                    || evenement.getType() == null || evenement.getDateDebut() == null
                    || evenement.getDateFin() == null || evenement.getLieu() == null
                    || evenement.getLieu().isBlank()) {
                log.warn("Événement incomplet conservé sans suppression (ID {}, titre {}).",
                        evenement.getId(), evenement.getTitre());
            }
        }

        LocalDateTime maintenant = LocalDateTime.now();
        creerEvenementDemonstration(
                "Atelier LIAS sur l’analyse de données scientifiques",
                "Atelier de démonstration consacré aux méthodes reproductibles d’analyse de données.",
                TypeEvenement.WORKSHOP, maintenant.minusMonths(11).withDayOfMonth(12).withHour(9).withMinute(0),
                maintenant.minusMonths(11).withDayOfMonth(12).withHour(16).withMinute(0),
                "Salle de séminaire LIAS", "Atelier LIAS 2025", sael, List.of(ouahabi));
        creerEvenementDemonstration(
                "Journée scientifique LIAS — systèmes intelligents",
                "Journée de démonstration LIAS autour des systèmes intelligents et de leurs applications.",
                TypeEvenement.CONFERENCE, maintenant.plusDays(24).withHour(9).withMinute(0),
                maintenant.plusDays(24).withHour(17).withMinute(0),
                "Université de Poitiers — LIAS", "Journée LIAS 2026", benabbou, List.of(sael, belangour));
        creerEvenementDemonstration(
                "Séminaire LIAS — sécurité et systèmes distribués",
                "Séminaire interne de démonstration sur la sécurité des systèmes distribués.",
                TypeEvenement.SEMINAIRE, maintenant.plusDays(7).withHour(14).withMinute(0),
                maintenant.plusDays(7).withHour(16).withMinute(0),
                "Salle de réunion LIAS", "Séminaires LIAS 2026", belangour, List.of(aitDaoud));
        creerEvenementAnnuleDemonstration(
                "Workshop LIAS — prototypage de recherche",
                "Session de démonstration sur le prototypage des résultats de recherche.",
                TypeEvenement.WORKSHOP, maintenant.minusDays(60).withHour(10).withMinute(0),
                maintenant.minusDays(60).withHour(15).withMinute(0),
                "Laboratoire LIAS", "Workshops LIAS 2026", ettaoufik,
                "Annulation de démonstration pour alimenter l’historique des événements.");
        creerEvenementDemonstration(
                "Conférence LIAS — méthodes de modélisation",
                "Conférence de démonstration sur les méthodes de modélisation et la validation expérimentale.",
                TypeEvenement.CONFERENCE, maintenant.minusMonths(5).withHour(9).withMinute(30),
                maintenant.minusMonths(5).withHour(12).withMinute(30),
                "Amphithéâtre LIAS", "Conférences LIAS 2026", aitDaoud, List.of(sael));
        creerEvenementDemonstration(
                "Workshop LIAS — ingénierie des systèmes",
                "Atelier de démonstration consacré à l’ingénierie et à l’évaluation des systèmes.",
                TypeEvenement.WORKSHOP, maintenant.plusMonths(2).withHour(9).withMinute(0),
                maintenant.plusMonths(2).withHour(16).withMinute(0),
                "Salle de travaux pratiques LIAS", "Workshops LIAS 2026", ouahabi, List.of(ettaoufik));

        log.info("Seeding des événements terminé; aucun événement existant n'a été supprimé.");
    }

    private void creerEvenementDemonstration(
            String titre, String description, TypeEvenement type,
            LocalDateTime debut, LocalDateTime fin, String lieu, String edition,
            Membre organisateur, List<Membre> participants) {
        if (organisateur == null || evenementRepository.existsByTitre(titre) || evenementRepository.count() >= 7) {
            return;
        }
        Evenement evenement = new Evenement();
        evenement.setTitre(titre);
        evenement.setDescription(description);
        evenement.setType(type);
        evenement.setDateDebut(debut);
        evenement.setDateFin(fin);
        evenement.setLieu(lieu);
        evenement.setEdition(edition);
        evenement.setCreePar(organisateur);
        evenement.getOrganisateurs().add(organisateur);
        participants.stream().filter(java.util.Objects::nonNull).forEach(evenement.getParticipants()::add);
        evenementRepository.save(evenement);
    }

    private void creerEvenementAnnuleDemonstration(
            String titre, String description, TypeEvenement type,
            LocalDateTime debut, LocalDateTime fin, String lieu, String edition,
            Membre organisateur, String motif) {
        if (organisateur == null || evenementRepository.existsByTitre(titre) || evenementRepository.count() >= 7) {
            return;
        }
        Evenement evenement = new Evenement();
        evenement.setTitre(titre);
        evenement.setDescription(description);
        evenement.setType(type);
        evenement.setDateDebut(debut);
        evenement.setDateFin(fin);
        evenement.setLieu(lieu);
        evenement.setEdition(edition);
        evenement.setCreePar(organisateur);
        evenement.setEstAnnule(true);
        evenement.setAnnulePar(organisateur);
        evenement.setMotifAnnulation(motif);
        evenement.setDateAnnulation(LocalDateTime.now());
        evenement.getOrganisateurs().add(organisateur);
        evenementRepository.save(evenement);
    }

    private void seedPublications() {
        Membre benabbou = membreRepository.findByEmail("f.benabbolias@gmail.com").orElse(null);
        Membre sael = membreRepository.findByEmail("n.sael@lias.ma").orElse(null);
        Membre belangour = membreRepository.findByEmail("a.belangour@lias.ma").orElse(null);
        
        if (publicationRepository.findByTitre("Artificial Intelligence for Quality of Life Study").isEmpty()
            && benabbou != null && sael != null) {
            Publication p1 = new Publication();
            p1.setTitre("Artificial Intelligence for Quality of Life Study");
            p1.setResume("Une étude sur l'impact de l'IA sur la qualité de vie, publiée dans IEEE Access.");
            p1.setDatePublication(java.time.LocalDate.of(2024, 1, 15));
            p1.setLienDoi("https://scholar.google.com/scholar?q=Artificial+Intelligence+for+Quality+of+Life+Study");
            p1.setType(com.lias.gestion_laboratoire.models.enums.TypePublication.ARTICLE_JOURNAL);
            p1.getAuteurs().add(benabbou);
            p1.getAuteurs().add(sael);
            // Assuming Jannani A. is not a seeded member, we only link Benabbou and Sael
            publicationRepository.save(p1);
        }

        if (publicationRepository.findByTitre("Hybrid Deep Learning for Bot Detection").isEmpty()
            && benabbou != null) {
            Publication p2 = new Publication();
            p2.setTitre("Hybrid Deep Learning for Bot Detection");
            p2.setResume("Détection de bots via un apprentissage profond hybride, publié dans IEEE Access.");
            p2.setDatePublication(java.time.LocalDate.of(2024, 3, 10));
            p2.setLienDoi("https://scholar.google.com/scholar?q=Hybrid+Deep+Learning+for+Bot+Detection");
            p2.setType(com.lias.gestion_laboratoire.models.enums.TypePublication.ARTICLE_JOURNAL);
            p2.getAuteurs().add(benabbou);
            // Ellaky Z. not seeded
            publicationRepository.save(p2);
        }
        
        if (publicationRepository.findByTitre("Breast Cancer Therapy Prediction").isEmpty()
            && sael != null) {
            Publication p3 = new Publication();
            p3.setTitre("Breast Cancer Therapy Prediction");
            p3.setResume("Prédiction de la thérapie du cancer du sein, publié dans IJOE.");
            p3.setDatePublication(java.time.LocalDate.of(2024, 5, 5));
            p3.setLienDoi("https://scholar.google.com/scholar?q=Breast+Cancer+Therapy+Prediction");
            p3.setType(com.lias.gestion_laboratoire.models.enums.TypePublication.ARTICLE_JOURNAL);
            p3.getAuteurs().add(sael); 
            publicationRepository.save(p3);
        }
        
        if (publicationRepository.findByTitre("Two-Stage Object Detection Models").isEmpty()
            && belangour != null) {
            Publication p4 = new Publication();
            p4.setTitre("Two-Stage Object Detection Models");
            p4.setResume("Modèles de détection d'objets en deux étapes, publié dans BEEI.");
            p4.setDatePublication(java.time.LocalDate.of(2024, 6, 20));
            p4.setLienDoi("https://scholar.google.com/scholar?q=Two-Stage+Object+Detection+Models");
            p4.setType(com.lias.gestion_laboratoire.models.enums.TypePublication.ARTICLE_JOURNAL);
            p4.getAuteurs().add(belangour);
            publicationRepository.save(p4);
        }

        creerPublicationDemonstration(
                "LIAS — Systèmes intelligents et analyse de données (démonstration 2025)",
                "Publication de démonstration pour le rapport annuel 2025.",
                LocalDate.of(2025, 6, 18),
                com.lias.gestion_laboratoire.models.enums.TypePublication.CONFERENCE,
                benabbou, belangour);
        creerPublicationDemonstration(
                "LIAS — Ingénierie des systèmes adaptatifs (démonstration 2026)",
                "Publication de démonstration pour le rapport annuel 2026.",
                LocalDate.of(2026, 4, 22),
                com.lias.gestion_laboratoire.models.enums.TypePublication.ARTICLE_JOURNAL,
                sael, belangour);
        
        publicationRepository.flush();
        log.info("Publications seedées avec succès.");
    }

    private void creerPublicationDemonstration(
            String titre, String resume, LocalDate datePublication,
            com.lias.gestion_laboratoire.models.enums.TypePublication type, Membre... auteurs) {
        if (publicationRepository.findByTitre(titre).isPresent()) {
            return;
        }
        Publication publication = new Publication();
        publication.setTitre(titre);
        publication.setResume(resume);
        publication.setDatePublication(datePublication);
        publication.setType(type);
        Stream.of(auteurs).filter(java.util.Objects::nonNull).forEach(publication.getAuteurs()::add);
        if (!publication.getAuteurs().isEmpty()) {
            publicationRepository.save(publication);
        }
    }

    private void seedReunions() throws Exception {
        Membre benabbou = membreRepository.findByEmail("f.benabbolias@gmail.com").orElse(null);
        Membre belangour = membreRepository.findByEmail("a.belangour@lias.ma").orElse(null);
        Membre sael = membreRepository.findByEmail("n.sael@lias.ma").orElse(null);
        Membre aitDaoud = membreRepository.findByEmail("m.aitdaoud@lias.ma").orElse(null);
        Membre ouahabi = membreRepository.findByEmail("s.ouahabi@lias.ma").orElse(null);
        Membre ettaoufik = membreRepository.findByEmail("a.ettaoufik@lias.ma").orElse(null);
        List<Membre> available = Stream.of(benabbou, belangour, sael, aitDaoud, ouahabi, ettaoufik)
                .filter(java.util.Objects::nonNull)
                .filter(membre -> membre.isCompteActif() && !membre.isCompteGele())
                .filter(membre -> membre.getStatut() == Statut.PERMANENT || membre.getStatut() == Statut.ASSOCIE)
                .toList();
        if (available.isEmpty()) {
            log.warn("Aucun membre permanent ou associé actif; aucune réunion de démonstration ajoutée.");
            return;
        }

        Membre fallbackOrganizer = available.stream()
                .filter(membre -> membre.getRole() == Role.DIRECTEUR || membre.getRole() == Role.VICE_DIRECTEUR
                        || membre.getRole() == Role.CHEF_EQUIPE)
                .findFirst().orElse(available.get(0));

        for (Reunion existing : reunionRepository.findAll()) {
            boolean changed = false;
            if (existing.getOrganisateur() == null) {
                existing.setOrganisateur(fallbackOrganizer);
                existing.setCreePar(fallbackOrganizer);
                existing.getParticipants().add(fallbackOrganizer);
                changed = true;
            }
            if (existing.getDateFin() == null && existing.getDateReunion() != null) {
                existing.setDateFin(existing.getDateReunion().plusHours(1));
                changed = true;
            }
            if (existing.getLieu() == null || existing.getLieu().isBlank()) {
                existing.setLieu("Salle de réunion LIAS (à confirmer)");
                changed = true;
            }
            if (existing.getOrdreDuJour() == null || existing.getOrdreDuJour().isBlank()) {
                existing.setOrdreDuJour("Ordre du jour à compléter par l'organisateur.");
                changed = true;
            }
            if (changed) reunionRepository.save(existing);
            if (existing.getProcesVerbal() != null
                    && existing.getProcesVerbal().getTypeDocument() != TypeDocument.PV) {
                log.warn("La réunion {} conserve son ancien document de type {} sans le requalifier.",
                        existing.getId(), existing.getProcesVerbal().getTypeDocument());
            }
        }

        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        seedReunion("Conseil de laboratoire LIAS", "Bilan des équipes; arbitrage des moyens; calendrier scientifique.",
                now.plusDays(14), "Salle du conseil LIAS", StatutReunion.PLANIFIEE, benabbou, available, null);
        seedReunion("Réunion d'équipe ISDIAC", "Avancement des projets; publications; encadrement doctoral.",
                now.plusDays(7), "Salle ISDIAC", StatutReunion.PLANIFIEE, sael, available, null);
        seedReunion("Réunion d'équipe SIMA", "Suivi des travaux; démonstrations; besoins de calcul.",
                now.plusDays(21), "Salle SIMA", StatutReunion.PLANIFIEE, aitDaoud, available, null);
        seedReunion("Réunion d'équipe SDTIC", "Coordination des projets; partenariats; échéances de soumission.",
                now.plusDays(28), "Salle SDTIC", StatutReunion.PLANIFIEE, ouahabi, available, null);
        Document pv = seedSamplePv(ettaoufik != null ? ettaoufik : fallbackOrganizer);
        seedReunion("Réunion ILIAS — compte rendu annuel", "Présentation des résultats; bilan des activités; priorités de l'année.",
                now.minusDays(35), "Salle ILIAS", StatutReunion.TERMINEE, ettaoufik, available, pv);
        seedReunion("Réunion budget et équipements", "Prévisions budgétaires; renouvellement du matériel; demandes de financement.",
                now.plusDays(18), "Salle de réunion LIAS", StatutReunion.ANNULEE, belangour, available, null);
    }

    private void seedConventions() {
        Partenaire um5 = seedPartenaire("Université Mohammed V — LRI", TypePartenaire.ACADEMIQUE, "Maroc");
        seedPartenaire("EMSI — LPRI", TypePartenaire.ACADEMIQUE, "Maroc");
        Partenaire liege = seedPartenaire("Université de Liège", TypePartenaire.ACADEMIQUE, "Belgique");
        seedPartenaire("ABA Technology Maroc", TypePartenaire.INDUSTRIEL, "Maroc");
        seedPartenaire("Tech-IT Maroc", TypePartenaire.INDUSTRIEL, "Maroc");
        seedPartenaire("Stefan cel Mare University, Suceava", TypePartenaire.ACADEMIQUE, "Roumanie");

        Membre director = membreRepository.findByEmail("f.benabbolias@gmail.com").orElse(null);
        if (director == null) {
            log.warn("Directeur manquant; aucune convention de démonstration ajoutée.");
            return;
        }
        seedConvention("Convention de recherche LIAS – LRI",
                "Collaboration scientifique, co-encadrement et échanges de chercheurs.",
                LocalDate.now().minusMonths(8), LocalDate.now().plusYears(3), um5, director);
        seedConvention("Accord de coopération LIAS – Université de Liège",
                "Échanges académiques et projets communs en systèmes d'information.",
                LocalDate.now().minusMonths(2), LocalDate.now().plusYears(2), liege, director);
    }

    private Partenaire seedPartenaire(String nom, TypePartenaire type, String pays) {
        return partenaireRepository.findAll().stream()
                .filter(partner -> partner.getNom().equalsIgnoreCase(nom))
                .findFirst()
                .orElseGet(() -> {
                    Partenaire partner = new Partenaire();
                    partner.setNom(nom);
                    partner.setType(type);
                    partner.setPays(pays);
                    return partenaireRepository.save(partner);
                });
    }

    private void seedConvention(String titre, String description, LocalDate signature, LocalDate expiration,
                                Partenaire partenaire, Membre director) {
        if (conventionRepository.existsByTitreIgnoreCaseAndPartenaireId(titre, partenaire.getId())) return;
        Convention convention = new Convention();
        convention.setTitre(titre);
        convention.setDescription(description);
        convention.setDateSignature(signature);
        convention.setDateExpiration(expiration);
        convention.setPartenaire(partenaire);
        convention.setCreePar(director);
        evenementRepository.findAll().stream().filter(event -> !event.isEstAnnule()).findFirst()
                .ifPresent(convention.getActivites()::add);
        conventionRepository.save(convention);
        log.info("Convention LIAS seedée : {}", titre);
    }

    private void seedLogistique() {
        Materiel posteGpu = seedMateriel("Station de travail GPU", "Calcul scientifique et entraînement de modèles.",
                "LIAS-INFO-001", "Informatique", EtatMateriel.BON_ETAT, 2, false);
        Materiel videoprojecteur = seedMateriel("Vidéoprojecteur de conférence", "Équipement audiovisuel partagé.",
                "LIAS-AUDIO-001", "Audiovisuel", EtatMateriel.BON_ETAT, 1, false);
        Materiel licences = seedMateriel("Licence logicielle de calcul", "Licence annuelle attribuable à une équipe.",
                "LIAS-SOFT-001", "Logiciels", EtatMateriel.NEUF, 12, true);
        seedMateriel("Fournitures de laboratoire", "Consommables pour les activités de recherche.",
                "LIAS-FOURN-001", "Fournitures", EtatMateriel.NEUF, 30, true);

        Membre directrice = membreRepository.findByEmail("f.benabbolias@gmail.com").orElse(null);
        Membre sael = membreRepository.findByEmail("n.sael@lias.ma").orElse(null);
        Membre aitDaoud = membreRepository.findByEmail("m.aitdaoud@lias.ma").orElse(null);
        Membre ouahabi = membreRepository.findByEmail("s.ouahabi@lias.ma").orElse(null);
        Membre ettaoufik = membreRepository.findByEmail("a.ettaoufik@lias.ma").orElse(null);

        seedDemande(sael, "Station de travail GPU", "En attente", posteGpu, StatutDemande.EN_ATTENTE, directrice);
        seedDemande(aitDaoud, "Licence logicielle de calcul", "Refusée", licences, StatutDemande.REFUSEE, directrice);
        seedDemande(ouahabi, "Vidéoprojecteur de conférence", "Validée", videoprojecteur, StatutDemande.VALIDEE, directrice);
        seedDemande(ettaoufik, "Station de travail GPU", "Retournée", posteGpu, StatutDemande.RETOURNEE, directrice);
    }

    private Materiel seedMateriel(String nom, String description, String inventory, String category,
                                  EtatMateriel condition, int quantity, boolean consumable) {
        return materielRepository.findAll().stream()
                .filter(item -> inventory.equalsIgnoreCase(item.getNumeroInventaire()))
                .findFirst()
                .orElseGet(() -> {
                    Materiel item = new Materiel();
                    item.setNom(nom);
                    item.setDescription(description);
                    item.setNumeroInventaire(inventory);
                    item.setCategorie(category);
                    item.setEtat(condition);
                    item.setQuantiteTotale(quantity);
                    item.setQuantiteDisponible(quantity);
                    item.setConsommable(consumable);
                    item.setDateAcquisition(LocalDate.now().minusMonths(5));
                    Membre director = membreRepository.findByEmail("f.benabbolias@gmail.com").orElse(null);
                    item.setCreePar(director);
                    Materiel saved = materielRepository.save(item);
                    log.info("Matériel LIAS seedé : {}", nom);
                    return saved;
                });
    }

    private void seedDemande(Membre requester, String need, String title, Materiel item,
                             StatutDemande status, Membre director) {
        if (requester == null || item == null || demandeMaterielRepository
                .existsByDemandeurIdAndNomBesoin(requester.getId(), title + " - " + need)) return;
        DemandeMateriel request = new DemandeMateriel();
        request.setDemandeur(requester);
        request.setNomBesoin(title + " - " + need);
        request.setDescriptionBesoin("Demande de démonstration LIAS : " + need + ".");
        request.setMotifDemande("Besoin de démonstration destiné au suivi des différents statuts logistiques.");
        request.setQuantiteDemandee(1);
        request.setQuantiteRetournee(status == StatutDemande.RETOURNEE ? 1 : 0);
        request.setMaterielDemande(item);
        request.setStatut(status);
        if (status != StatutDemande.EN_ATTENTE) {
            request.setDateReponse(LocalDateTime.now().minusDays(7));
            request.setTraitePar(director);
        }
        if (status == StatutDemande.VALIDEE || status == StatutDemande.RETOURNEE) {
            request.setMaterielAssigne(item);
        }
        if (status == StatutDemande.REFUSEE) {
            request.setMotifRefus("Exemple de motif : priorité donnée aux demandes urgentes.");
        }
        demandeMaterielRepository.save(request);
        if (status == StatutDemande.VALIDEE && item.getQuantiteDisponible() > 0) {
            item.setQuantiteDisponible(item.getQuantiteDisponible() - 1);
            if (item.isConsommable()) item.setQuantiteTotale(item.getQuantiteTotale() - 1);
            materielRepository.save(item);
        }
        log.info("Demande logistique LIAS seedée : {} ({})", title, status);
    }

    private void seedReunion(String titre, String ordreDuJour, LocalDateTime dateDebut, String lieu,
                             StatutReunion statut, Membre organisateur, List<Membre> available, Document pv) {
        if (organisateur == null || reunionRepository.existsByTitre(titre)) return;
        Reunion reunion = new Reunion();
        reunion.setTitre(titre);
        reunion.setOrdreDuJour(ordreDuJour);
        reunion.setDateReunion(dateDebut);
        reunion.setDateFin(dateDebut.plusHours(1));
        reunion.setLieu(lieu);
        reunion.setStatut(statut);
        reunion.setOrganisateur(organisateur);
        reunion.setCreePar(organisateur);
        reunion.setStatutChangePar(organisateur);
        reunion.setDateChangementStatut(LocalDateTime.now());
        reunion.getParticipants().add(organisateur);
        available.stream().limit(5).forEach(reunion.getParticipants()::add);
        if (pv != null) reunion.setProcesVerbal(pv);
        reunion = reunionRepository.save(reunion);
        if (pv != null) {
            ReunionProcesVerbal history = new ReunionProcesVerbal();
            history.setReunion(reunion);
            history.setDocument(pv);
            history.setAjoutePar(organisateur);
            reunionProcesVerbalRepository.save(history);
        }
        log.info("Réunion LIAS seedée : {}", titre);
    }

    private Document seedSamplePv(Membre deposant) throws Exception {
        String filename = "pv-reunion-ilias-demo.pdf";
        String storagePath = "uploads/documents/" + filename;
        Path filePath = Paths.get(storagePath);
        Files.createDirectories(filePath.getParent());
        boolean validSample = Files.exists(filePath)
                && Files.readString(filePath, StandardCharsets.US_ASCII).contains("/Count 1");
        if (!validSample) {
            List<String> pdfObjects = new ArrayList<>(List.of(
                    "<< /Type /Catalog /Pages 2 0 R >>",
                    "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                            + "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                    "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));
            String stream = "BT /F1 16 Tf 50 740 Td (LIAS - Meeting minutes) Tj "
                    + "0 -28 Td /F1 11 Tf (Annual activity review and decisions.) Tj ET";
            pdfObjects.add("<< /Length " + stream.length() + " >>\nstream\n" + stream + "\nendstream");
            StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
            List<Integer> offsets = new ArrayList<>();
            for (int index = 0; index < pdfObjects.size(); index++) {
                offsets.add(pdf.length());
                pdf.append(index + 1).append(" 0 obj\n")
                        .append(pdfObjects.get(index)).append("\nendobj\n");
            }
            int xrefOffset = pdf.length();
            pdf.append("xref\n0 ").append(pdfObjects.size() + 1).append("\n")
                    .append("0000000000 65535 f \n");
            for (int offset : offsets) {
                pdf.append(String.format("%010d 00000 n \n", offset));
            }
            pdf.append("trailer\n<< /Size ").append(pdfObjects.size() + 1)
                    .append(" /Root 1 0 R >>\nstartxref\n")
                    .append(xrefOffset).append("\n%%EOF\n");
            Files.writeString(filePath, pdf, StandardCharsets.US_ASCII);
        }
        Document document = documentRepository.findAll().stream()
                .filter(candidate -> filename.equals(candidate.getNomFichier()))
                .findFirst().orElse(null);
        if (document != null) {
            document.setCheminStockage(storagePath);
            document.setContentType("application/pdf");
            document.setTaille(Files.size(filePath));
            document.setTypeDocument(TypeDocument.PV);
            return documentRepository.save(document);
        }
        document = new Document();
        document.setNomFichier(filename);
        document.setCheminStockage(storagePath);
        document.setContentType("application/pdf");
        document.setTaille(Files.size(filePath));
        document.setTypeDocument(TypeDocument.PV);
        document.setDeposePar(deposant);
        return documentRepository.save(document);
    }

    private void seedMembre(String nom, String prenom, String email, Role role,
                            EquipeLaboratoire equipe, RoleUtilisateur roleUtilisateur, String password) {
        if (membreRepository.findByEmail(email).isEmpty()) {
            Membre m = new Membre();
            m.setNom(nom);
            m.setPrenom(prenom);
            m.setEmail(email);
            m.setRole(role);
            m.setEquipeLaboratoire(equipe);
            m.setRoleSysteme(roleUtilisateur);
            m.setMotDePasse(password);
            m.setStatut(Statut.PERMANENT);
            m.setTitreAcademique("Pr.");
            m.setCompteActif(true);
            m.setCompteGele(false);
            membreRepository.save(m);
            log.info("Membre seedé : {} {}", prenom, nom);
        }
    }

    private void configurerRoleViceDirecteur() {
        membreRepository.findByEmail("a.belangour@lias.ma").ifPresent(membre -> {
            membre.setRole(Role.VICE_DIRECTEUR);
            membre.setRoleSysteme(RoleUtilisateur.VICE_DIRECTEUR);
            membre.setEquipeLaboratoire(EquipeLaboratoire.AUCUNE);
            membreRepository.save(membre);
        });
    }

    private void seedCompteAdministrateur() {
        Membre administrateur = membreRepository.findByEmail("admin@lias.ma").orElse(null);
        if (administrateur == null) {
            administrateur = new Membre();
            administrateur.setNom("LIAS");
            administrateur.setPrenom("Administrateur");
            administrateur.setEmail("admin@lias.ma");
            administrateur.setMotDePasse(passwordEncoder.encode(ADMIN_INITIAL_PASSWORD));
            administrateur.setStatut(Statut.PERMANENT);
            administrateur.setTitreAcademique("Admin");
            administrateur.setCompteActif(true);
            administrateur.setCompteGele(false);
        }

        administrateur.setRole(Role.ADMIN);
        administrateur.setRoleSysteme(RoleUtilisateur.ADMIN);
        administrateur.setEquipeLaboratoire(EquipeLaboratoire.AUCUNE);
        membreRepository.save(administrateur);
    }

    private void seedMandats() {
            Membre directrice = membreRepository.findByEmail("f.benabbolias@gmail.com").orElse(null);
            if (directrice == null || !directrice.isCompteActif()) {
                log.warn("Seed des mandats ignoré : compte de la directrice introuvable ou inactif.");
                return;
            }
            LocalDate debutCourant = LocalDate.of(2023, 1, 1);
            seedMandatSiPosteLibre(directrice, Role.DIRECTEUR, null, debutCourant, directrice);

            seedMandatParEmail("a.belangour@lias.ma", Role.VICE_DIRECTEUR, null, debutCourant, directrice);
            seedMandatParEmail("n.sael@lias.ma", Role.CHEF_EQUIPE, EquipeLaboratoire.ISDIAC, debutCourant, directrice);
            seedMandatParEmail("m.aitdaoud@lias.ma", Role.CHEF_EQUIPE, EquipeLaboratoire.SIMA, debutCourant, directrice);
            seedMandatParEmail("s.ouahabi@lias.ma", Role.CHEF_EQUIPE, EquipeLaboratoire.SDTIC, debutCourant, directrice);
            seedMandatParEmail("a.ettaoufik@lias.ma", Role.CHEF_EQUIPE, EquipeLaboratoire.ILIAS, debutCourant, directrice);

            boolean historiqueDirecteur = mandatRepository.findAll().stream()
                    .anyMatch(m -> m.getResponsabilite() == Role.DIRECTEUR && m.getDateFin() != null);
            if (!historiqueDirecteur) {
                Mandat demonstration = new Mandat();
                demonstration.setTitulaire(directrice);
                demonstration.setResponsabilite(Role.DIRECTEUR);
                demonstration.setDateDebut(LocalDate.of(2019, 1, 1));
                demonstration.setDateFin(LocalDate.of(2022, 12, 31));
                demonstration.setAttribuePar(directrice);
                demonstration.setCloturePar(directrice);
                demonstration.setDateCloture(LocalDateTime.of(2022, 12, 31, 12, 0));
                demonstration.setMotifCloture("Passation — historique de démonstration à vérifier avec les dates officielles.");
                demonstration.setDescription("Donnée de démonstration, dates historiques à confirmer.");
                mandatRepository.save(demonstration);
            }
        }

    private void seedMandatParEmail(String email, Role role, EquipeLaboratoire equipe,
                                    LocalDate dateDebut, Membre auteur) {
        membreRepository.findByEmail(email).filter(Membre::isCompteActif)
                .ifPresent(m -> seedMandatSiPosteLibre(m, role, equipe, dateDebut, auteur));
    }

    private void seedMandatSiPosteLibre(Membre titulaire, Role role, EquipeLaboratoire equipe,
                                        LocalDate dateDebut, Membre auteur) {
        boolean posteOccupe = mandatRepository.findAll().stream()
                .anyMatch(m -> m.getResponsabilite() == role
                        && (role != Role.CHEF_EQUIPE || m.getEquipe() == equipe)
                        && !m.getDateDebut().isAfter(LocalDate.now())
                        && (m.getDateFin() == null || !m.getDateFin().isBefore(LocalDate.now())));
        if (posteOccupe) return;
        Mandat mandat = new Mandat();
        mandat.setTitulaire(titulaire);
        mandat.setResponsabilite(role);
        mandat.setEquipe(equipe);
        mandat.setDateDebut(dateDebut);
        mandat.setAttribuePar(auteur);
        mandat.setDescription("Mandat initialisé par le seeder LIAS.");
        mandatRepository.save(mandat);
        log.info("Mandat seedé : {} {} - {}", titulaire.getPrenom(), titulaire.getNom(), role);
    }
}
