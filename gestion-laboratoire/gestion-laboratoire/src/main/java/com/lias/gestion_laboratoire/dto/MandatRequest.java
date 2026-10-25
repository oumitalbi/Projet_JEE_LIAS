package com.lias.gestion_laboratoire.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.Role;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MandatRequest {
    @NotNull
    private Long membreId;

    @NotNull
    private Role responsabilite;

    private EquipeLaboratoire equipe;

    @NotNull
    @PastOrPresent
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateDebut;

    @Size(max = 2000)
    private String description;
}
