package com.lias.gestion_laboratoire.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DemandeMaterielRequest(
        Long materielId,
        @Size(max = 200) String nomBesoin,
        @NotNull @Min(1) Integer quantite,
        @NotBlank @Size(max = 2000) String justification) {
}
