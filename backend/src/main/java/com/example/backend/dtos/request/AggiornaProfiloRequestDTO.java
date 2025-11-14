package com.example.backend.dtos.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AggiornaProfiloRequestDTO {
    @Size(max = 100, message = "Nome completo non può superare 100 caratteri")
    private String nomeCompleto;

    @Size(max = 100, message = "Bio non può superare 100 caratteri")
    private String bio;

    private String profilePictureUrl; // URL Firebase
}