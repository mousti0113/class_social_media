package com.example.backend.dtos.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegistrazioneRequestDTO {
    @NotBlank(message = "Username è obbligatorio")
    @Size(min = 3, max = 50, message = "Username deve essere tra 3 e 50 caratteri")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username può contenere solo lettere, numeri e underscore")
    private String username;

    @NotBlank(message = "Email è obbligatoria")
    @Email(message = "Formato email non valido")
    private String email;

    @NotBlank(message = "Password è obbligatoria")
    @Size(min = 6, message = "Password deve essere almeno 6 caratteri")
    private String password;

    @NotBlank(message = "Nome completo è obbligatorio")
    @Size(max = 100)
    private String nomeCompleto;
}