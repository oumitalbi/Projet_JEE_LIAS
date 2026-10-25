package com.lias.gestion_laboratoire.dtos;

import com.lias.gestion_laboratoire.models.enums.EquipeLaboratoire;
import com.lias.gestion_laboratoire.models.enums.Statut;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DemandeCreateRequest {
    @NotBlank
    @Size(max = 100)
    private String nom;

    @NotBlank
    @Size(max = 100)
    private String prenom;

    @NotBlank
    @Email
    @Size(max = 150)
    private String email;

    @NotNull
    private Statut statutSouhaite;

    @NotBlank
    @Size(max = 200)
    private String etablissementOrigine;

    @Size(max = 200)
    private String laboratoireOrigine;

    @NotNull
    private EquipeLaboratoire equipeSouhaitee;

    @NotBlank
    @Size(min = 50, max = 10000)
    private String lettreMotivation;

    @Size(max = 5000)
    private String centresInteret;

    @Size(max = 5000)
    private String biographie;

    @NotNull
    private MultipartFile cv;

    @Size(max = 200)
    private String website;

    @AssertTrue(message = "Votre consentement au traitement des données est obligatoire.")
    private boolean consentementDonnees;
}
