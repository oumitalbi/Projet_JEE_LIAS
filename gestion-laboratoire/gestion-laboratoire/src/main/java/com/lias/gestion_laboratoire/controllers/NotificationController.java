package com.lias.gestion_laboratoire.controllers;

import com.lias.gestion_laboratoire.dto.NotificationResponse;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.Notification;
import com.lias.gestion_laboratoire.services.NotificationService;
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
 * Contrôleur REST sécurisé pour les notifications du laboratoire LIAS.
 *
 * <p>Base URL : {@code /api/notifications}</p>
 *
 * <p>Toutes les requêtes de consultation et de mise à jour s'appuient
 * sur le membre extrait du JWT pour empêcher toute usurpation d'identité.</p>
 *
 * @author LIAS Laboratory
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class NotificationController {

    private final NotificationService notificationService;

    // =========================================================================
    // ENDPOINTS BASÉS SUR LE JWT DU MEMBRE CONNECTÉ (/api/notifications/moi)
    // =========================================================================

    /**
     * Récupère toutes les notifications actives (non archivées) du membre connecté.
     */
    @GetMapping("/moi")
    public ResponseEntity<List<NotificationResponse>> obtenirMesNotifications() {
        Membre moi = currentMembre();
        List<NotificationResponse> list = notificationService.obtenirMesNotifications(moi).stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * Récupère les notifications non lues du membre connecté.
     */
    @GetMapping("/moi/non-lues")
    public ResponseEntity<List<NotificationResponse>> obtenirMesNotificationsNonLues() {
        Membre moi = currentMembre();
        List<NotificationResponse> list = notificationService.obtenirMesNotificationsNonLues(moi).stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * Renvoie le compteur de notifications non lues (pour le badge de la cloche).
     */
    @GetMapping("/moi/compteur")
    public ResponseEntity<Map<String, Long>> compterMesNotificationsNonLues() {
        Membre moi = currentMembre();
        long count = notificationService.compterMesNotificationsNonLues(moi);
        return ResponseEntity.ok(Map.of("nonLues", count));
    }

    /**
     * Marque toutes les notifications du membre connecté comme lues.
     */
    @PutMapping("/moi/marquer-lues")
    public ResponseEntity<Void> marquerToutesMesNotificationsCommeLues() {
        Membre moi = currentMembre();
        notificationService.marquerToutesMesNotificationsCommeLues(moi);
        return ResponseEntity.noContent().build();
    }

    /**
     * Marque une notification spécifique comme lue.
     */
    @PutMapping("/{id}/marquer-lue")
    public ResponseEntity<NotificationResponse> marquerNotificationCommeLue(@PathVariable("id") Long id) {
        Membre moi = currentMembre();
        Notification n = notificationService.marquerNotificationCommeLue(id, moi);
        return ResponseEntity.ok(NotificationResponse.from(n));
    }

    /**
     * Archive logiquement une notification (soft delete selon la section 21).
     */
    @PutMapping("/{id}/archiver")
    public ResponseEntity<NotificationResponse> archiverNotification(@PathVariable("id") Long id) {
        Membre moi = currentMembre();
        Notification n = notificationService.archiverNotification(id, moi);
        return ResponseEntity.ok(NotificationResponse.from(n));
    }

    // =========================================================================
    // ENDPOINTS AVEC {membreId} (SÉCURISÉS : VÉRIFIENT L'ÉGALITÉ AVEC LE JWT)
    // =========================================================================

    @GetMapping("/{membreId}/non-lues")
    public ResponseEntity<List<NotificationResponse>> obtenirNotificationsNonLues(
            @PathVariable("membreId") Long membreId) {
        verifierIdentite(membreId);
        List<NotificationResponse> list = notificationService.obtenirNotificationsNonLues(membreId).stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{membreId}")
    public ResponseEntity<List<NotificationResponse>> obtenirToutesNotifications(
            @PathVariable("membreId") Long membreId) {
        verifierIdentite(membreId);
        List<NotificationResponse> list = notificationService.obtenirToutesNotifications(membreId).stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{membreId}/marquer-lues")
    public ResponseEntity<Void> marquerToutesCommeLues(
            @PathVariable("membreId") Long membreId) {
        verifierIdentite(membreId);
        notificationService.marquerToutesCommeLues(membreId);
        return ResponseEntity.noContent().build();
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

    private void verifierIdentite(Long membreId) {
        Membre moi = currentMembre();
        if (!moi.getId().equals(membreId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Accès refusé : vous ne pouvez consulter que vos propres notifications.");
        }
    }
}
