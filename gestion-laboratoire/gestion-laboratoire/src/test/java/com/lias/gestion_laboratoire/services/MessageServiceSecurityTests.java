package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.authorization.AuthorizationService;
import com.lias.gestion_laboratoire.models.Membre;
import com.lias.gestion_laboratoire.models.Message;
import com.lias.gestion_laboratoire.models.enums.TypeMessage;
import com.lias.gestion_laboratoire.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceSecurityTests {

    @Mock private MessageRepository messageRepository;
    @Mock private MessageLectureRepository messageLectureRepository;
    @Mock private MembreRepository membreRepository;
    @Mock private EvenementRepository evenementRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private NotificationService notificationService;
    @InjectMocks private MessageService messageService;

    @Test
    void onlyDirectRecipientCanMarkMessageAsRead() {
        Membre recipient = member(2L);
        Membre sender = member(1L);
        Message message = new Message();
        message.setId(10L);
        message.setTypeMessage(TypeMessage.DIRECT);
        message.setExpediteur(sender);
        message.setDestinataire(recipient);
        when(messageRepository.findById(10L)).thenReturn(Optional.of(message));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> messageService.marquerCommeLu(10L, sender));

        assertEquals(403, exception.getStatusCode().value());
        verify(messageRepository, never()).save(message);
        verify(messageLectureRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private Membre member(Long id) {
        Membre member = new Membre();
        member.setId(id);
        return member;
    }
}
