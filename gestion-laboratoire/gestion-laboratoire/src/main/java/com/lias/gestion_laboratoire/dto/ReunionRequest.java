package com.lias.gestion_laboratoire.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class ReunionRequest {

    @NotBlank(message = "Le titre est obligatoire.")
    @Size(max = 200, message = "Le titre ne peut pas dépasser 200 caractères.")
    private String titre;

    @NotBlank(message = "L'ordre du jour est obligatoire.")
    private String ordreDuJour;

    @NotNull(message = "La date de début est obligatoire.")
    @FutureOrPresent(message = "La date de début ne peut pas être dans le passé.")
    private LocalDateTime dateDebut;

    @NotNull(message = "La date de fin est obligatoire.")
    private LocalDateTime dateFin;

    @NotBlank(message = "Le lieu est obligatoire.")
    @Size(max = 255, message = "Le lieu ne peut pas dépasser 255 caractères.")
    private String lieu;

    @NotEmpty(message = "Au moins un participant invité est obligatoire.")
    private Set<Long> participantsIds;

    @jakarta.validation.constraints.AssertTrue(message = "La date de fin doit être postérieure ou égale à la date de début.")
    public boolean isDateFinValide() {
        return dateDebut == null || dateFin == null || !dateFin.isBefore(dateDebut);
    }
}
