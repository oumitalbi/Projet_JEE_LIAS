package com.lias.gestion_laboratoire.dtos;

import com.lias.gestion_laboratoire.models.enums.StatutAdhesion;

import java.time.LocalDateTime;

public record DemandeAdhesionReceipt(Long id, StatutAdhesion statut, LocalDateTime dateDemande,
                                     String message) {
}
