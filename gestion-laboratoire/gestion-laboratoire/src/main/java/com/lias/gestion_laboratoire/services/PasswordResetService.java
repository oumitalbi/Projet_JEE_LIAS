package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.PasswordResetToken;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import com.lias.gestion_laboratoire.repositories.PasswordResetTokenRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(15);
    private static final Duration REQUEST_COOLDOWN = Duration.ofMinutes(1);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final MembreRepository membreRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional
    public void demanderReinitialisation(String email) {
        membreRepository.findByEmailIgnoreCase(email.trim())
                .filter(this::compteEligible)
                .ifPresent(membre -> envoyerLien(membre, false, false, false));
    }

    @Transactional
    public void envoyerLienParAdministration(Long membreId) {
        Membre membre = membreRepository.findById(membreId)
                .orElseThrow(() -> new EntityNotFoundException("Membre introuvable."));
        if (!compteEligible(membre)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible d’envoyer un lien à un compte inactif ou gelé.");
        }
        envoyerLien(membre, false, true, true);
    }

    @Transactional
    public void envoyerLienInitialisation(String email) {
        Membre membre = membreRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new EntityNotFoundException("Membre introuvable."));
        envoyerLien(membre, true, true, false);
    }

    @Transactional
    public void reinitialiserMotDePasse(String tokenBrut, String nouveauMotDePasse) {
        PasswordResetToken token = tokenRepository.findByTokenHash(sha256(tokenBrut))
                .orElseThrow(this::tokenInvalide);
        LocalDateTime now = LocalDateTime.now();
        if (token.getUtiliseLe() != null || token.getRevoqueLe() != null || token.getExpireLe().isBefore(now)
                || !compteEligible(token.getMembre())) {
            throw tokenInvalide();
        }

        Membre membre = token.getMembre();
        membre.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        membre.setSessionVersion(membre.getSessionVersion() + 1);
        membreRepository.save(membre);

        token.setUtiliseLe(now);
        tokenRepository.save(token);
        tokenRepository.findByMembre_IdAndRevoqueLeIsNullAndUtiliseLeIsNull(membre.getId())
                .stream()
                .filter(autre -> !autre.getId().equals(token.getId()))
                .forEach(autre -> autre.setRevoqueLe(now));
    }

    private void envoyerLien(Membre membre, boolean initialisation, boolean contournerDelai, boolean signalerEchecMail) {
        LocalDateTime now = LocalDateTime.now();
        var dernierJeton = tokenRepository.findFirstByMembre_IdOrderByDateCreationDesc(membre.getId());
        if (!contournerDelai && dernierJeton.isPresent()
                && dernierJeton.get().getDateCreation() != null
                && dernierJeton.get().getDateCreation().plus(REQUEST_COOLDOWN).isAfter(now)) {
            return;
        }

        tokenRepository.findByMembre_IdAndRevoqueLeIsNullAndUtiliseLeIsNull(membre.getId())
                .forEach(jeton -> jeton.setRevoqueLe(now));

        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String tokenBrut = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken token = new PasswordResetToken();
        token.setMembre(membre);
        token.setTokenHash(sha256(tokenBrut));
        token.setExpireLe(now.plus(TOKEN_LIFETIME));
        tokenRepository.save(token);

        String baseUrl = frontendUrl.replaceAll("/+$", "");
        String lien = baseUrl + "/reset-password?token=" + tokenBrut;
        try {
            emailService.envoyerLienReinitialisation(membre.getEmail(), lien, initialisation);
        } catch (MailException exception) {
            token.setRevoqueLe(LocalDateTime.now());
            tokenRepository.save(token);
            if (signalerEchecMail) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Le courriel n’a pas pu être envoyé. Réessayez plus tard.", exception);
            }
            log.error("Le courriel de réinitialisation n’a pas pu être envoyé; demande conservée sans révéler l’existence du compte.");
        }
    }

    private boolean compteEligible(Membre membre) {
        return membre.isCompteActif() && !membre.isCompteGele()
                && membre.getStatut() != Statut.RETRAITE
                && membre.getStatut() != Statut.ANCIEN;
    }

    private ResponseStatusException tokenInvalide() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Le lien est invalide ou expiré. Demandez un nouveau lien de réinitialisation.");
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 n’est pas disponible sur cette plateforme.", exception);
        }
    }
}
