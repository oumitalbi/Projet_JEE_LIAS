package com.lias.gestion_laboratoire.dtos;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String motDePasse;
}
