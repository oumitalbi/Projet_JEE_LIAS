package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.PasswordResetToken;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import com.lias.gestion_laboratoire.repositories.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTests {

    @Mock
    private MembreRepository membreRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void configureFrontendUrl() {
        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "https://lias.example");
    }

    @Test
    void unknownEmailReceivesNoMailAndDoesNotExposeAccountExistence() {
        when(membreRepository.findByEmailIgnoreCase("inconnu@example.com")).thenReturn(Optional.empty());

        passwordResetService.demanderReinitialisation("inconnu@example.com");

        verify(emailService, never()).envoyerLienReinitialisation(anyString(), anyString(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void resetTokenIsStoredAsHashAndSentInLink() throws Exception {
        Membre membre = membre();
        when(membreRepository.findByEmailIgnoreCase(membre.getEmail())).thenReturn(Optional.of(membre));
        when(tokenRepository.findFirstByMembre_IdOrderByDateCreationDesc(membre.getId())).thenReturn(Optional.empty());
        when(tokenRepository.findByMembre_IdAndRevoqueLeIsNullAndUtiliseLeIsNull(membre.getId())).thenReturn(List.of());
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.demanderReinitialisation(membre.getEmail());

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).envoyerLienReinitialisation(
                org.mockito.ArgumentMatchers.eq(membre.getEmail()), linkCaptor.capture(), org.mockito.ArgumentMatchers.eq(false));
        String rawToken = linkCaptor.getValue().substring(linkCaptor.getValue().indexOf("?token=") + 7);
        String expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(rawToken.getBytes(StandardCharsets.UTF_8)));

        assertEquals(expectedHash, tokenCaptor.getValue().getTokenHash());
        assertNotEquals(rawToken, tokenCaptor.getValue().getTokenHash());
        assertEquals("https://lias.example/reset-password?token=" + rawToken, linkCaptor.getValue());
    }

    @Test
    void successfulResetUpdatesPasswordConsumesTokenAndInvalidatesExistingSessions() {
        Membre membre = membre();
        PasswordResetToken token = new PasswordResetToken();
        token.setId(8L);
        token.setMembre(membre);
        token.setExpireLe(LocalDateTime.now().plusMinutes(5));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(tokenRepository.findByMembre_IdAndRevoqueLeIsNullAndUtiliseLeIsNull(membre.getId()))
                .thenReturn(List.of(token));
        when(passwordEncoder.encode("correct horse battery staple")).thenReturn("bcrypt-hash");
        when(membreRepository.save(membre)).thenReturn(membre);

        passwordResetService.reinitialiserMotDePasse("one-time-token", "correct horse battery staple");

        assertEquals("bcrypt-hash", membre.getMotDePasse());
        assertEquals(1, membre.getSessionVersion());
        assertEquals(true, token.getUtiliseLe() != null);
        verify(membreRepository).save(membre);
        verify(tokenRepository).save(token);
    }

    @Test
    void expiredResetTokenIsRejected() {
        PasswordResetToken token = new PasswordResetToken();
        token.setMembre(membre());
        token.setExpireLe(LocalDateTime.now().minusSeconds(1));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThrows(ResponseStatusException.class,
                () -> passwordResetService.reinitialiserMotDePasse("expired-token", "correct horse battery staple"));

        verify(membreRepository, never()).save(any(Membre.class));
    }

    private Membre membre() {
        Membre membre = new Membre();
        membre.setId(17L);
        membre.setEmail("membre@example.com");
        membre.setMotDePasse("old-hash");
        membre.setStatut(Statut.PERMANENT);
        membre.setCompteActif(true);
        membre.setCompteGele(false);
        return membre;
    }
}
