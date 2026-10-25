package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.dtos.TraitementAdhesionResponse;
import com.lias.gestion_laboratoire.dtos.TraitementRequest;
import com.lias.gestion_laboratoire.models.DemandeAdhesion;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.RoleUtilisateur;
import com.lias.gestion_laboratoire.models.enums.Statut;
import com.lias.gestion_laboratoire.models.enums.StatutAdhesion;
import com.lias.gestion_laboratoire.repositories.DemandeAdhesionRepository;
import com.lias.gestion_laboratoire.repositories.MembreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class DemandeAdhesionDecisionTests {
    @Autowired
    private DemandeAdhesionService adhesionService;

    @Autowired
    private MembreRepository membreRepository;

    @Autowired
    private DemandeAdhesionRepository demandeRepository;

    @MockBean
    private EmailService emailService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void refusalIsPersistedAndListedEvenWhenEmailDeliveryFails() {
        Membre directeur = new Membre();
        directeur.setNom("Directeur");
        directeur.setPrenom("Test");
        directeur.setEmail("director-" + System.nanoTime() + "@example.invalid");
        directeur.setMotDePasse("encoded");
        directeur.setStatut(Statut.PERMANENT);
        directeur.setRole(Role.MEMBRE_EQUIPE);
        directeur.setRoleSysteme(RoleUtilisateur.ADMIN);
        directeur.setEquipeLaboratoire(EquipeLaboratoire.DIRECTION);
        directeur.setCompteActif(true);
        directeur.setCompteGele(false);
        directeur = membreRepository.saveAndFlush(directeur);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(directeur, null, java.util.List.of()));

        DemandeAdhesion demande = new DemandeAdhesion();
        demande.setNom("Talbi");
        demande.setPrenom("Oumaima");
        demande.setEmail("candidate-" + System.nanoTime() + "@example.invalid");
        demande.setLettreMotivation("Motivation de test suffisamment longue.");
        demande.setStatutSouhaite(Statut.DOCTORANT);
        demande.setEtablissementOrigine("Université de test");
        demande.setEquipeSouhaitee(EquipeLaboratoire.SIMA);
        demande.setConsentementDonnees(true);
        demande.setStatut(StatutAdhesion.EN_ATTENTE);
        demande = demandeRepository.saveAndFlush(demande);
        Long demandeId = demande.getId();
        String demandeEmail = demande.getEmail();

        doThrow(new MailSendException("SMTP unavailable"))
                .when(emailService).envoyerRefusAdhesion(demandeEmail, "Motif de refus de test");
        TraitementRequest request = new TraitementRequest();
        request.setEstAcceptee(false);
        request.setMotif("Motif de refus de test");

        TraitementAdhesionResponse response = adhesionService.traiterDemande(demandeId, request);

        assertFalse(response.notificationEnvoyee());
        assertEquals(StatutAdhesion.REFUSEE, demandeRepository.findById(demandeId).orElseThrow().getStatut());
        assertEquals(1, adhesionService.obtenirHistorique().stream()
                .filter(item -> item.id().equals(demandeId)).count());
        assertEquals(0, adhesionService.obtenirDemandesEnAttente().stream()
                .filter(item -> item.id().equals(demandeId)).count());
    }
}
