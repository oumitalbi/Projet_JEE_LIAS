package com.lias.gestion_laboratoire.dtos;

import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.Role;
import com.lias.gestion_laboratoire.models.enums.Statut;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TraitementRequest {
    @NotNull
    private Boolean estAcceptee;

    private Statut statutAccorde;
    private EquipeLaboratoire equipeAccordee;
    private Role roleInitial;

    @Size(max = 2000)
    private String motif;
}
