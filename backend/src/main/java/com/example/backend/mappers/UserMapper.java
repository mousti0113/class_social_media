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

    private Boolean isUserOnline(User user) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        return user.getSessions().stream()
                .anyMatch(session -> session.getIsOnline() &&
                        session.getLastActivity().isAfter(threshold));
    }
}
