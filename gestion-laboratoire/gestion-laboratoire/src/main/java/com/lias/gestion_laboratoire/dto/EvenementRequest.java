package com.lias.gestion_laboratoire.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lias.gestion_laboratoire.models.enums.TypeEvenement;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class EvenementRequest {

    @NotBlank(message = "Le titre est obligatoire.")
    @Size(max = 200, message = "Le titre ne peut pas dépasser 200 caractères.")
    private String titre;

    private String description;

    @NotNull(message = "Le type est obligatoire.")
    private TypeEvenement type;

    @NotNull(message = "La date de début est obligatoire.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateDebut;

    @NotNull(message = "La date de fin est obligatoire.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateFin;

    @NotBlank(message = "Le lieu est obligatoire.")
    private String lieu;

    private String edition;

    @NotEmpty(message = "Au moins un organisateur doit être sélectionné.")
    private Set<Long> organisateursIds;

    @AssertTrue(message = "La date de fin doit être égale ou postérieure à la date de début.")
    public boolean isDateFinValide() {
        return dateDebut == null || dateFin == null || !dateFin.isBefore(dateDebut);
    }
}
