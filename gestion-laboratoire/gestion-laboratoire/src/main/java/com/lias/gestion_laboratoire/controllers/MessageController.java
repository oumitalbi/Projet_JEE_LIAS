package com.lias.gestion_laboratoire.controllers;

import com.lias.gestion_laboratoire.dto.*;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.services.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST sécurisé pour la messagerie interne du laboratoire LIAS.
 *
 * <p>Base URL : {@code /api/messages}</p>
 *
 * <p>L'expéditeur d'un message est systématiquement dérivé du JWT de la session
 * active, garantissant qu'aucun membre ne peut envoyer de message sous l'identité
 * d'un tiers. L'accès aux conversations est également cloisonné.</p>
 *
 * @author LIAS Laboratory
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class MessageController {

    private final MessageService messageService;

    // =========================================================================
    // COMPTEURS (BADGE MESSAGERIE)
    // =========================================================================

    /**
     * Récupère le décompte des messages et notifications non lus pour le membre connecté.
     */
    @GetMapping("/moi/compteur")
    public ResponseEntity<CompteurNonLusResponse> obtenirCompteurNonLus() {
        Membre moi = currentMembre();
        return ResponseEntity.ok(messageService.compterNonLus(moi));
    }

    // =========================================================================
    // CONVERSATIONS DIRECTES
    // =========================================================================

    /**
     * Récupère la liste des conversations directes du membre connecté.
     */
    @GetMapping("/moi/conversations")
    public ResponseEntity<List<ConversationResume>> obtenirConversations() {
        Membre moi = currentMembre();
        return ResponseEntity.ok(messageService.obtenirConversations(moi));
    }

    /**
     * Récupère le fil de discussion complet avec un autre membre et marque les messages reçus comme lus.
     */
    @GetMapping("/conversation/{autreMembreId}")
    public ResponseEntity<List<MessageResponse>> obtenirDiscussionDirecte(
            @PathVariable("autreMembreId") Long autreMembreId) {
        Membre moi = currentMembre();
        return ResponseEntity.ok(messageService.obtenirDiscussionDirecte(moi, autreMembreId));
    }

    /**
     * Marque tous les messages directs provenant d'un membre comme lus.
     */
    @PutMapping("/conversation/{autreMembreId}/marquer-lus")
    public ResponseEntity<Void> marquerDiscussionCommeLue(
            @PathVariable("autreMembreId") Long autreMembreId) {
        Membre moi = currentMembre();
        messageService.marquerDiscussionCommeLue(moi, autreMembreId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // CANAUX D'ÉQUIPE ET ÉVÉNEMENTS
    // =========================================================================

    /**
     * Récupère les messages d'une équipe de recherche (réservé aux permanents membres de cette équipe ou superviseurs).
     */
    @GetMapping("/equipe/{equipe}")
    public ResponseEntity<List<MessageResponse>> obtenirMessagesEquipe(
            @PathVariable("equipe") EquipeLaboratoire equipe) {
        Membre moi = currentMembre();
        return ResponseEntity.ok(messageService.obtenirMessagesEquipe(moi, equipe));
    }

    /**
     * Récupère le fil de discussion d'un événement scientifique.
     */
    @GetMapping("/evenement/{evenementId}")
    public ResponseEntity<List<MessageResponse>> obtenirMessagesEvenement(
            @PathVariable("evenementId") Long evenementId) {
        Membre moi = currentMembre();
        return ResponseEntity.ok(messageService.obtenirMessagesEvenement(moi, evenementId));
    }

    // =========================================================================
    // ENVOI DE MESSAGES (EXPÉDITEUR = JWT)
    // =========================================================================

    /**
     * Envoie un message direct. L'expéditeur est extrait automatiquement du JWT.
     */
    @PostMapping("/envoyer/direct")
    public ResponseEntity<MessageResponse> envoyerMessageDirect(
            @Valid @RequestBody EnvoyerMessageDirectRequest request) {
        Membre moi = currentMembre();
        MessageResponse response = messageService.envoyerMessageDirect(
                moi, request.destinataireId(), request.contenu());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Envoie un message dans le canal d'une équipe de recherche.
     */
    @PostMapping("/envoyer/equipe")
    public ResponseEntity<MessageResponse> envoyerMessageEquipe(
            @Valid @RequestBody EnvoyerMessageEquipeRequest request) {
        Membre moi = currentMembre();
        MessageResponse response = messageService.envoyerMessageEquipe(
                moi, request.equipe(), request.contenu());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Envoie un message dans la discussion d'un événement scientifique.
     */
    @PostMapping("/envoyer/evenement")
    public ResponseEntity<MessageResponse> envoyerMessageEvenement(
            @Valid @RequestBody EnvoyerMessageEvenementRequest request) {
        Membre moi = currentMembre();
        MessageResponse response = messageService.envoyerMessageEvenement(
                moi, request.evenementId(), request.contenu());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Marque un message individuel comme lu pour le membre connecté.
     */
    @PutMapping("/{id}/lire")
    public ResponseEntity<Void> marquerCommeLu(@PathVariable("id") Long id) {
        Membre moi = currentMembre();
        messageService.marquerCommeLu(id, moi);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // ANCIENNES ROUTES SÉCURISÉES POUR COMPATIBILITÉ
    // =========================================================================

    @GetMapping("/recus/{membreId}")
    public ResponseEntity<List<MessageResponse>> lireMesMessages(
            @PathVariable("membreId") Long membreId) {
        Membre moi = currentMembre();
        if (!moi.getId().equals(membreId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Accès refusé : vous ne pouvez consulter que vos propres messages.");
        }
        List<MessageResponse> messages = messageService.lireMesMessages(moi);
        return ResponseEntity.ok(messages);
    }

    // =========================================================================
    // UTILITAIRES PRIVÉS DE SÉCURITÉ
    // =========================================================================

    private Membre currentMembre() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Membre m) {
            return m;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise.");
    }
}
