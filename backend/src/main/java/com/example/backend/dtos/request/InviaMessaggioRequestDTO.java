package com.example.backend.dtos.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class InviaMessaggioRequestDTO {
    @NotNull(message = "ID destinatario è obbligatorio")
    private Long destinatarioId;

    @NotBlank(message = "Contenuto messaggio è obbligatorio")
    @Size(min = 1, max = 5000, message = "Il messaggio deve essere tra 1 e 5000 caratteri")
    private String contenuto;


}