package com.lias.gestion_laboratoire.authorization;

import com.lias.gestion_laboratoire.models.Document;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.Statut;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationServiceDocumentTests {

    private final AuthorizationService authorizationService = new AuthorizationService();

    @Test
    void deniesDocumentAccessToDoctorant() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authorizationService.verifierAccesDocuments(membre(1L, Statut.DOCTORANT)));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void deniesUploadByAssociate() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authorizationService.verifierDepotDocument(membre(1L, Statut.ASSOCIE)));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void allowsUploaderToArchiveOwnDocument() {
        Membre deposant = membre(1L, Statut.PERMANENT);
        Document document = new Document();
        document.setDeposePar(deposant);

        authorizationService.verifierGestionDocument(deposant, document);
    }

    @Test
    void deniesAnotherPermanentMemberFromArchivingDocument() {
        Membre autreMembre = membre(2L, Statut.PERMANENT);
        Document document = new Document();
        document.setDeposePar(membre(1L, Statut.PERMANENT));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authorizationService.verifierGestionDocument(autreMembre, document));

        assertEquals(403, exception.getStatusCode().value());
    }

    private Membre membre(Long id, Statut statut) {
        Membre membre = new Membre();
        membre.setId(id);
        membre.setStatut(statut);
        membre.setRole(Role.MEMBRE_EQUIPE);
        membre.setCompteActif(true);
        membre.setCompteGele(false);
        return membre;
    }
}
