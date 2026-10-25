package com.lias.gestion_laboratoire.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public void envoyerLienReinitialisation(String email, String lien, boolean initialisation) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(initialisation
                ? "Finalisez la création de votre accès LIAS"
                : "Réinitialisation de votre mot de passe LIAS");
        message.setText("Bonjour,\n\n" +
                (initialisation
                        ? "Votre compte LIAS est prêt. Choisissez votre mot de passe en ouvrant ce lien :\n"
                        : "Une demande de réinitialisation de mot de passe a été reçue. Ouvrez ce lien pour en choisir un nouveau :\n") +
                lien + "\n\n" +
                "Ce lien est à usage unique et expire dans 15 minutes. Si vous n'êtes pas à l'origine de cette demande, ignorez ce message.\n\n" +
                "L'équipe du laboratoire LIAS.");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Échec d’envoi du lien de réinitialisation (adresse masquée).", e);
            throw e;
        }
    }

    public void envoyerRefusAdhesion(String email, String motif) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Suite à votre demande d'adhésion au LIAS");
        message.setText("Bonjour,\n\n"
                + "Nous vous remercions pour votre candidature au laboratoire LIAS. "
                + "Après examen, votre demande n'a pas été retenue.\n\n"
                + (motif == null || motif.isBlank() ? "" : "Motif communiqué : " + motif.trim() + "\n\n")
                + "Cette décision n'empêche pas une nouvelle candidature ultérieure.\n\n"
                + "L'équipe du laboratoire LIAS.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error("Échec d’envoi de la décision d’adhésion (adresse masquée).", exception);
            throw exception;
        }
    }
}
