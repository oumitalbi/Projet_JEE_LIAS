package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.authorization.AuthorizationService;
import com.lias.gestion_laboratoire.dto.*;
import com.lias.gestion_laboratoire.models.*;
import com.lias.gestion_laboratoire.models.enums.*;
import com.lias.gestion_laboratoire.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service métier pour la messagerie interne du laboratoire LIAS.
 *
 * <p>Prend en charge :
 * <ul>
 *   <li>Les messages directs (entre membres permanents et associés)</li>
 *   <li>Les canaux d'équipe (réservés aux membres permanents d'une équipe)</li>
 *   <li>Les fils de discussion d'événements scientifiques</li>
 *   <li>Le suivi de lecture individuel via {@link MessageLecture}</li>
 * </ul>
 * </p>
 *
 * @author LIAS Laboratory
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageLectureRepository messageLectureRepository;
    private final MembreRepository membreRepository;
    private final EvenementRepository evenementRepository;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;

    // =========================================================================
    // COMPTEURS DE NON LUS (POUR LES BADGES TOPBAR ET SIDEBAR)
    // =========================================================================

    /**
     * Calcule le nombre total de messages et notifications non lus pour le membre connecté.
     */
    @Transactional(readOnly = true)
    public CompteurNonLusResponse compterNonLus(Membre membre) {
        if (membre.getStatut() == Statut.DOCTORANT || membre.getRoleSysteme() == RoleUtilisateur.DOCTORANT) {
            return new CompteurNonLusResponse(0, 0, 0, 0, 0);
        }

        long notifNonLues = notificationService.compterMesNotificationsNonLues(membre);
        long directNonLus = messageRepository.countDirectNonLus(membre.getId());

        long equipeNonLus = 0;
        if (membre.getStatut() == Statut.PERMANENT && membre.getEquipeLaboratoire() != null
                && membre.getEquipeLaboratoire() != EquipeLaboratoire.AUCUNE) {
            equipeNonLus = messageRepository.countUnreadTeamForMember(
                    membre.getId(), membre.getEquipeLaboratoire());
        }

        boolean supervision = membre.getRole() == Role.ADMIN || membre.getRole() == Role.DIRECTEUR
                || membre.getRole() == Role.VICE_DIRECTEUR || membre.getRoleSysteme() == RoleUtilisateur.ADMIN;
        long evenementNonLus = messageRepository.countUnreadEventsForMember(membre.getId(), membre, supervision);

        long messagesTotal = directNonLus + equipeNonLus + evenementNonLus;
        return new CompteurNonLusResponse(notifNonLues, messagesTotal, directNonLus, equipeNonLus, evenementNonLus);
    }

    // =========================================================================
    // CONVERSATIONS DIRECTES (LISTE GROUPÉE)
    // =========================================================================

    /**
     * Retourne la liste des conversations directes avec le dernier message et le nombre de non lus.
     */
    @Transactional(readOnly = true)
    public List<ConversationResume> obtenirConversations(Membre membre) {
        authorizationService.verifierAccesMessagerieInterne(membre);

        List<Message> allDirect = messageRepository.findAllDirectMessagesForMember(membre.getId());
        Map<Long, List<Message>> parContact = new LinkedHashMap<>();

        for (Message m : allDirect) {
            Long contactId = Objects.equals(m.getExpediteur().getId(), membre.getId())
                    ? (m.getDestinataire() != null ? m.getDestinataire().getId() : null)
                    : m.getExpediteur().getId();
            if (contactId != null) {
                parContact.computeIfAbsent(contactId, k -> new ArrayList<>()).add(m);
            }
        }

        List<ConversationResume> result = new ArrayList<>();
        for (Map.Entry<Long, List<Message>> entry : parContact.entrySet()) {
            Long contactId = entry.getKey();
            List<Message> msgs = entry.getValue();
            Message dernier = msgs.get(0); // trié par dateEnvoi DESC

            Membre contact = Objects.equals(dernier.getExpediteur().getId(), contactId)
                    ? dernier.getExpediteur()
                    : dernier.getDestinataire();

            if (contact != null) {
                long nonLus = msgs.stream()
                        .filter(m -> Objects.equals(m.getExpediteur().getId(), contactId) && !m.isLu())
                        .count();

                result.add(new ConversationResume(
                        contact.getId(),
                        contact.getNom(),
                        contact.getPrenom(),
                        contact.getEmail(),
                        contact.getPhoto(),
                        contact.getRole() != null ? contact.getRole().name() : "",
                        dernier.getContenu(),
                        dernier.getDateEnvoi(),
                        nonLus
                ));
            }
        }

        return result;
    }

    /**
     * Récupère le fil complet des échanges directs avec un membre et marque les messages reçus comme lus.
     */
    public List<MessageResponse> obtenirDiscussionDirecte(Membre membre, Long autreMembreId) {
        authorizationService.verifierAccesMessagerieInterne(membre);

        Membre autre = membreRepository.findById(autreMembreId)
                .orElseThrow(() -> new EntityNotFoundException("Membre introuvable : " + autreMembreId));

        List<Message> discussion = messageRepository.findDiscussionDirecte(membre.getId(), autreMembreId);

        // Marquer comme lus les messages reçus de l'autre membre
        boolean modified = false;
        for (Message m : discussion) {
            if (Objects.equals(m.getExpediteur().getId(), autreMembreId) && !m.isLu()) {
                m.setLu(true);
                modified = true;
            }
        }
        if (modified) {
            messageRepository.saveAll(discussion);
        }

        return discussion.stream()
                .map(m -> MessageResponse.from(m, true))
                .toList();
    }

    /**
     * Marque tous les messages directs provenant d'un membre comme lus.
     */
    public void marquerDiscussionCommeLue(Membre membre, Long autreMembreId) {
        List<Message> discussion = messageRepository.findDiscussionDirecte(membre.getId(), autreMembreId);
        List<Message> nonLus = discussion.stream()
                .filter(m -> Objects.equals(m.getExpediteur().getId(), autreMembreId) && !m.isLu())
                .peek(m -> m.setLu(true))
                .toList();
        if (!nonLus.isEmpty()) {
            messageRepository.saveAll(nonLus);
        }
    }

    // =========================================================================
    // MESSAGES D'ÉQUIPE (CANAL DE GROUPE)
    // =========================================================================

    /**
     * Récupère les messages d'un canal d'équipe et marque comme lus ceux non encore lus par ce membre.
     */
    public List<MessageResponse> obtenirMessagesEquipe(Membre membre, EquipeLaboratoire equipe) {
        authorizationService.verifierEnvoiMessageEquipe(membre, equipe);

        List<Message> messages = messageRepository.findMessagesEquipe(equipe);
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> messageIds = messages.stream().map(Message::getId).toList();
        Set<Long> readIds = messageLectureRepository.findReadMessageIds(membre.getId(), messageIds);

        // Enregistrer la lecture pour les messages non encore lus
        List<MessageLecture> newLectures = new ArrayList<>();
        for (Message m : messages) {
            if (!readIds.contains(m.getId()) && !Objects.equals(m.getExpediteur().getId(), membre.getId())) {
                newLectures.add(new MessageLecture(m, membre));
            }
        }
        if (!newLectures.isEmpty()) {
            messageLectureRepository.saveAll(newLectures);
        }

        return messages.stream()
                .map(m -> MessageResponse.from(m, readIds.contains(m.getId())
                        || Objects.equals(m.getExpediteur().getId(), membre.getId())
                        || newLectures.stream().anyMatch(nl -> nl.getMessage().getId().equals(m.getId()))))
                .toList();
    }

    // =========================================================================
    // MESSAGES D'ÉVÉNEMENT (DISCUSSION LIÉE)
    // =========================================================================

    /**
     * Récupère le fil de discussion d'un événement et marque comme lus les messages pour ce membre.
     */
    public List<MessageResponse> obtenirMessagesEvenement(Membre membre, Long evenementId) {
        Evenement evenement = evenementRepository.findById(evenementId)
                .orElseThrow(() -> new EntityNotFoundException("Événement introuvable : " + evenementId));

        authorizationService.verifierAccesDiscussionEvenement(membre, evenement);

        List<Message> messages = messageRepository.findMessagesEvenement(evenementId);
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> messageIds = messages.stream().map(Message::getId).toList();
        Set<Long> readIds = messageLectureRepository.findReadMessageIds(membre.getId(), messageIds);

        List<MessageLecture> newLectures = new ArrayList<>();
        for (Message m : messages) {
            if (!readIds.contains(m.getId()) && !Objects.equals(m.getExpediteur().getId(), membre.getId())) {
                newLectures.add(new MessageLecture(m, membre));
            }
        }
        if (!newLectures.isEmpty()) {
            messageLectureRepository.saveAll(newLectures);
        }

        return messages.stream()
                .map(m -> MessageResponse.from(m, readIds.contains(m.getId())
                        || Objects.equals(m.getExpediteur().getId(), membre.getId())
                        || newLectures.stream().anyMatch(nl -> nl.getMessage().getId().equals(m.getId()))))
                .toList();
    }

    // =========================================================================
    // ENVOI DE MESSAGES (SÉCURISÉ PAR LE JWT)
    // =========================================================================

    /**
     * Envoie un message direct d'un membre connecté vers un autre membre actif.
     */
    public MessageResponse envoyerMessageDirect(Membre expediteur, Long destinataireId, String contenu) {
        authorizationService.verifierAccesMessagerieInterne(expediteur);

        if (Objects.equals(expediteur.getId(), destinataireId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous ne pouvez pas vous envoyer de message à vous-même.");
        }

        Membre destinataire = membreRepository.findById(destinataireId)
                .orElseThrow(() -> new EntityNotFoundException("Destinataire introuvable : " + destinataireId));

        if (destinataire.getStatut() == Statut.ANCIEN || destinataire.getStatut() == Statut.RETRAITE
                || !destinataire.isCompteActif() || destinataire.isCompteGele()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le destinataire est inactif.");
        }

        if (destinataire.getStatut() == Statut.DOCTORANT || destinataire.getRoleSysteme() == RoleUtilisateur.DOCTORANT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La messagerie interne n'est pas ouverte aux doctorants (section 2.2.C).");
        }

        Message message = new Message();
        message.setTypeMessage(TypeMessage.DIRECT);
        message.setExpediteur(expediteur);
        message.setDestinataire(destinataire);
        message.setContenu(contenu.trim());
        message.setLu(false);
        message.setArchivee(false);

        Message saved = messageRepository.save(message);

        // Notification envoyée au destinataire
        notificationService.creerNotification(
                destinataire.getId(),
                "Nouveau message de " + expediteur.getPrenom() + " " + expediteur.getNom(),
                contenu.length() > 80 ? contenu.substring(0, 77) + "..." : contenu,
                TypeNotification.MESSAGE,
                "MESSAGE",
                expediteur.getId()
        );

        return MessageResponse.from(saved, true);
    }

    /**
     * Envoie un message à toute une équipe de recherche.
     */
    public MessageResponse envoyerMessageEquipe(Membre expediteur, EquipeLaboratoire equipe, String contenu) {
        authorizationService.verifierEnvoiMessageEquipe(expediteur, equipe);

        Message message = new Message();
        message.setTypeMessage(TypeMessage.EQUIPE);
        message.setExpediteur(expediteur);
        message.setEquipeDestinataire(equipe);
        message.setContenu(contenu.trim());
        message.setLu(false);
        message.setArchivee(false);

        Message saved = messageRepository.save(message);

        // L'expéditeur est automatiquement considéré comme ayant lu son propre message
        messageLectureRepository.save(new MessageLecture(saved, expediteur));

        return MessageResponse.from(saved, true);
    }

    /**
     * Envoie un message dans le fil de discussion d'un événement scientifique.
     */
    public MessageResponse envoyerMessageEvenement(Membre expediteur, Long evenementId, String contenu) {
        Evenement evenement = evenementRepository.findById(evenementId)
                .orElseThrow(() -> new EntityNotFoundException("Événement introuvable : " + evenementId));

        authorizationService.verifierAccesDiscussionEvenement(expediteur, evenement);

        Message message = new Message();
        message.setTypeMessage(TypeMessage.EVENEMENT);
        message.setExpediteur(expediteur);
        message.setEvenement(evenement);
        message.setContenu(contenu.trim());
        message.setLu(false);
        message.setArchivee(false);

        Message saved = messageRepository.save(message);

        // L'expéditeur a lu son propre message
        messageLectureRepository.save(new MessageLecture(saved, expediteur));

        return MessageResponse.from(saved, true);
    }

    /**
     * Marque un message individuel comme lu pour le membre connecté.
     */
    public void marquerCommeLu(Long messageId, Membre membre) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message introuvable : " + messageId));

        if (message.isArchivee()) {
            throw new EntityNotFoundException("Message introuvable : " + messageId);
        }

        if (message.getTypeMessage() == TypeMessage.DIRECT) {
            if (message.getDestinataire() == null
                    || !Objects.equals(message.getDestinataire().getId(), membre.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Seul le destinataire peut marquer ce message comme lu.");
            }
            message.setLu(true);
            messageRepository.save(message);
            return;
        }

        if (message.getTypeMessage() == TypeMessage.EQUIPE) {
            authorizationService.verifierEnvoiMessageEquipe(membre, message.getEquipeDestinataire());
        } else if (message.getTypeMessage() == TypeMessage.EVENEMENT) {
            authorizationService.verifierAccesDiscussionEvenement(membre, message.getEvenement());
        }

        if (!messageLectureRepository.existsByMessage_IdAndLecteur_Id(messageId, membre.getId())) {
            messageLectureRepository.save(new MessageLecture(message, membre));
        }
    }

    // =========================================================================
    // MÉTHODES DE COMPATIBILITÉ
    // =========================================================================

    public Message envoyerMessageDirect(Long expediteurId, Long destinataireId, String contenu) {
        Membre expediteur = membreRepository.findById(expediteurId)
                .orElseThrow(() -> new EntityNotFoundException("Expéditeur introuvable : " + expediteurId));
        MessageResponse res = envoyerMessageDirect(expediteur, destinataireId, contenu);
        return messageRepository.findById(res.id()).orElse(null);
    }

    public Message envoyerMessageEquipe(Long expediteurId, EquipeLaboratoire equipe, String contenu) {
        Membre expediteur = membreRepository.findById(expediteurId)
                .orElseThrow(() -> new EntityNotFoundException("Expéditeur introuvable : " + expediteurId));
        MessageResponse res = envoyerMessageEquipe(expediteur, equipe, contenu);
        return messageRepository.findById(res.id()).orElse(null);
    }

    public Message envoyerMessageEvenement(Long expediteurId, Long evenementId, String contenu) {
        Membre expediteur = membreRepository.findById(expediteurId)
                .orElseThrow(() -> new EntityNotFoundException("Expéditeur introuvable : " + expediteurId));
        MessageResponse res = envoyerMessageEvenement(expediteur, evenementId, contenu);
        return messageRepository.findById(res.id()).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> lireMesMessages(Membre membre) {
        authorizationService.verifierAccesMessagerieInterne(membre);
        return messageRepository.findByDestinataire_IdAndArchiveeFalseOrderByDateEnvoiDesc(membre.getId())
                .stream()
                .map(message -> MessageResponse.from(message, message.isLu()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Message> lireMesMessages(Long membreId) {
        return messageRepository.findByDestinataire_IdAndArchiveeFalseOrderByDateEnvoiDesc(membreId);
    }

    @Transactional(readOnly = true)
    public List<Message> lireMessagesEvenement(Long evenementId) {
        return messageRepository.findByEvenement_IdOrderByDateEnvoiAsc(evenementId);
    }

    public Message marquerCommeLu(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message introuvable : " + messageId));
        message.setLu(true);
        return messageRepository.save(message);
    }
}
