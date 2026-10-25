package com.lias.gestion_laboratoire.dtos;

import com.lias.gestion_laboratoire.models.enums.RoleUtilisateur;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String token;
    private Long id;
    private String nom;
    private String prenom;
    private RoleUtilisateur role;
}
