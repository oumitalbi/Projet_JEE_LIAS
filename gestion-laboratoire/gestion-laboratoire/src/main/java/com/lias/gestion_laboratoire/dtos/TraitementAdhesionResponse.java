package com.lias.gestion_laboratoire.dtos;

public record TraitementAdhesionResponse(
        DemandeAdhesionResponse demande,
        boolean notificationEnvoyee,
        String message) {
}
