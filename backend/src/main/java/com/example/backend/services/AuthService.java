package com.example.backend.services;

import com.example.backend.dtos.request.LoginRequestDTO;
import com.example.backend.dtos.request.RegistrazioneRequestDTO;
import com.example.backend.dtos.request.RefreshTokenRequestDTO;
import com.example.backend.dtos.response.LoginResponseDTO;
import com.example.backend.dtos.response.RefreshTokenResponseDTO;
import com.example.backend.mappers.UserMapper;
import com.example.backend.models.RefreshToken;
import com.example.backend.models.User;
import com.example.backend.repositories.UserRepository;
import com.example.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service per la gestione dell'autenticazione e registrazione utenti.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    /**
     * Registra un nuovo utente
     */
    @Transactional
    public LoginResponseDTO registrazione(RegistrazioneRequestDTO request) {
        // Valida limite studenti (17 max)
        userRepository.validateStudentLimit();

        // Verifica username unico
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username già in uso");
        }

        // Verifica email unica
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email già in uso");
        }

        // Crea nuovo utente
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getNomeCompleto())
                .isAdmin(false) // Per default non è admin
                .isActive(true)
                .build();

        user = userRepository.save(user);

        // Genera token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.creaRefreshToken(user.getId());

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .type("Bearer")
                .user(userMapper.toUtenteResponseDTO(user))
                .build();
    }

    /**
     * Effettua il login
     */
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {
        // Autentica l'utente
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // Carica i dettagli utente
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

        // Genera token
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.creaRefreshToken(user.getId());

        // Aggiorna last seen
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .type("Bearer")
                .user(userMapper.toUtenteResponseDTO(user))
                .build();
    }

    /**
     * Rinnova l'access token usando il refresh token
     */
    @Transactional
    public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        return refreshTokenService.verificaRefreshToken(request.getRefreshToken())
                .map(refreshToken -> {
                    User user = refreshToken.getUser();
                    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

                    // Genera nuovo access token
                    String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);

                    // Opzionale: rigenera anche il refresh token per sicurezza
                    RefreshToken newRefreshToken = refreshTokenService.creaRefreshToken(user.getId());

                    return RefreshTokenResponseDTO.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(newRefreshToken.getToken())
                            .tipo("Bearer")
                            .build();
                })
                .orElseThrow(() -> new RuntimeException("Refresh token non valido o scaduto"));
    }

    /**
     * Effettua il logout invalidando tutti i refresh token dell'utente
     */
    @Transactional
    public void logout(Long userId) {
        refreshTokenService.eliminaRefreshTokenUtente(userId);
    }
}