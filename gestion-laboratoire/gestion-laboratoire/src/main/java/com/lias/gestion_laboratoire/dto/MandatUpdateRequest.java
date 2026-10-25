package com.lias.gestion_laboratoire.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MandatUpdateRequest {
    @NotNull
    @PastOrPresent
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateDebut;
    private EquipeLaboratoire equipe;
    @Size(max = 2000)
    private String description;
}
