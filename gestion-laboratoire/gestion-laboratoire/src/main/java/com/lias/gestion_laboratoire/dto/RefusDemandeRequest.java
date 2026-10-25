package com.lias.gestion_laboratoire.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefusDemandeRequest(@NotBlank @Size(max = 2000) String motif) {
}
