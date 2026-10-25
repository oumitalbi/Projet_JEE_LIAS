package com.lias.gestion_laboratoire.controllers;

import com.lias.gestion_laboratoire.dtos.LoginRequest;
import com.lias.gestion_laboratoire.dtos.LoginResponse;
import com.lias.gestion_laboratoire.dto.PasswordResetConfirmRequest;
import com.lias.gestion_laboratoire.dto.PasswordResetRequest;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import com.lias.gestion_laboratoire.security.JwtUtil;
import com.lias.gestion_laboratoire.services.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MembreRepository membreRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PasswordResetService passwordResetService;

    @PostMapping("/mot-de-passe-oublie")
    public ResponseEntity<Map<String, String>> demanderReinitialisation(
            @Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.demanderReinitialisation(request.email());
        return ResponseEntity.accepted().body(Map.of("message",
                "Si cette adresse correspond à un compte actif, un lien de réinitialisation va être envoyé."));
    }

    @PostMapping("/reinitialiser-mot-de-passe")
    public ResponseEntity<Map<String, String>> reinitialiserMotDePasse(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.reinitialiserMotDePasse(request.token(), request.motDePasse());
        return ResponseEntity.ok(Map.of("message", "Votre mot de passe a été réinitialisé."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Optional<Membre> membreOpt = membreRepository.findByEmail(loginRequest.getEmail());

        if (membreOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiants incorrects.");
        }

        Membre membre = membreOpt.get();

        // Sécurité supplémentaire : On vérifie que le compte n'est pas bloqué ou gelé
        if (!membre.isCompteActif() || membre.isCompteGele()
                || membre.getStatut() == Statut.RETRAITE
                || membre.getStatut() == Statut.ANCIEN) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Votre compte est inactif ou gelé.");
        }

        if (passwordEncoder.matches(loginRequest.getMotDePasse(), membre.getMotDePasse())) {
            String roleStr = membre.getRoleSysteme() != null ? membre.getRoleSysteme().name() : "USER";
            
            // Génération du JWT avec l'email et le rôle
            String token = jwtUtil.generateToken(membre.getEmail(), roleStr, membre.getSessionVersion());

            // Préparation de la réponse avec les infos de base
            LoginResponse response = new LoginResponse(
                    token,
                    membre.getId(),
                    membre.getNom(),
                    membre.getPrenom(),
                    membre.getRoleSysteme()
            );

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiants incorrects.");
        }
    }
}
