package com.lias.gestion_laboratoire.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record CalendrierItemResponse(
        Long id,
        String source,
        String titre,
        String type,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dateDebut,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dateFin,
        String lieu,
        String statut,
        boolean annule,
        boolean concerneMembre,
        String description,
        List<Long> organisateursIds,
        List<Long> participantsIds
) {}
