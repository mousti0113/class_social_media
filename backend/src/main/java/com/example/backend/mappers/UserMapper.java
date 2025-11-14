package com.example.backend.mappers;

import com.example.backend.dtos.response.UserSummaryDTO;
import com.example.backend.dtos.response.UserResponseDTO;
import com.example.backend.models.User;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class UserMapper {

    public UserResponseDTO toUtenteResponseDTO(User user) {
        if (user == null) return null;

        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nomeCompleto(user.getFullName())
                .bio(user.getBio())
                .profilePictureUrl(user.getProfilePictureUrl())
                .isAdmin(user.getIsAdmin())
                .isActive(user.getIsActive())
                .lastSeen(user.getLastSeen())
                .isOnline(isUserOnline(user))
                .build();
    }

    public UserSummaryDTO toUtenteSummaryDTO(User user) {
        if (user == null) return null;

        return UserSummaryDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nomeCompleto(user.getFullName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .isOnline(isUserOnline(user))
                .build();
    }

    /**
     * Determina se un utente è online in base all'ultimo accesso.
     * <p>
     * Un utente è considerato online se il suo lastSeen è negli ultimi 5 minuti.
     * <p>
     * PERFORMANCE: Usa il campo lastSeen già presente nell'entità User invece
     * di caricare lazy la collezione sessions. Questo evita N+1 query quando
     * si mappano liste di utenti.
     * <p>
 
     *
     * @param user L'utente da verificare
     * @return true se online, false altrimenti
     */
    private Boolean isUserOnline(User user) {
        if (user.getLastSeen() == null) {
            return false;
        }

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        return user.getLastSeen().isAfter(threshold);
    }
}
