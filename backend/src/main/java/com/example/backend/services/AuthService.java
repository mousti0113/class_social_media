package com.example.backend.services;

import com.example.backend.dtos.request.LoginRequestDTO;
import com.example.backend.dtos.request.RegistrazioneRequestDTO;
import com.example.backend.dtos.request.RefreshTokenRequestDTO;
import com.example.backend.dtos.response.LoginResponseDTO;
import com.example.backend.dtos.response.RefreshTokenResponseDTO;
import com.example.backend.exception.InvalidCredentialsException;
import com.example.backend.exception.InvalidTokenException;
import com.example.backend.exception.ResourceAlreadyExistsException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.mappers.UserMapper;
import com.example.backend.models.RefreshToken;
import com.example.backend.models.User;
import com.example.backend.repositories.UserRepository;
import com.example.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private static  final String AUTHORIZATION_TYPE="Bearer";

    /**
     * Registra un nuovo utente
     */
    @Transactional
    public LoginResponseDTO registrazione(RegistrazioneRequestDTO request) {
        log.info("Tentativo di registrazione per username: {}", request.getUsername());

        // Valida limite studenti (17 max)
        userRepository.validateStudentLimit();

        // Verifica username unico
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Tentativo di registrazione con username già esistente: {}", request.getUsername());
            throw new ResourceAlreadyExistsException("Utente", "username", request.getUsername());
        }

        // Verifica email unica
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Tentativo di registrazione con email già esistente: {}", request.getEmail());
            throw new ResourceAlreadyExistsException("Utente", "email", request.getEmail());
        }

        // Crea nuovo utente
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getNomeCompleto())
                .isAdmin(false)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("Utente registrato con successo: {} (ID: {})", user.getUsername(), user.getId());

        // Genera token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.creaRefreshToken(user.getId());

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .type(AUTHORIZATION_TYPE)
                .user(userMapper.toUtenteResponseDTO(user))
                .build();
    }

    /**
     * Effettua il login
     */
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {
        log.info("Tentativo di login per username: {}", request.getUsername());

        try {
            // Autentica l'utente
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            log.warn("Login fallito per username: {} - Credenziali errate", request.getUsername());
            throw new InvalidCredentialsException();
        }

        // Carica i dettagli utente
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.error("Utente non trovato dopo autenticazione: {}", request.getUsername());
                    return new ResourceNotFoundException("Utente", "username", request.getUsername());
                });

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

        // Genera token
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.creaRefreshToken(user.getId());

        // Aggiorna last seen
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);

        log.info("Login effettuato con successo per utente: {} (ID: {})", user.getUsername(), user.getId());

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .type(AUTHORIZATION_TYPE)
                .user(userMapper.toUtenteResponseDTO(user))
                .build();
    }

    /**
     * Rinnova l'access token usando il refresh token
     */
    @Transactional
    public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        log.info("Tentativo di refresh token");

        return refreshTokenService.verificaRefreshToken(request.getRefreshToken())
                .map(refreshToken -> {
                    User user = refreshToken.getUser();
                    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

                    // Genera nuovo access token
                    String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);

                    // Rigenera anche il refresh token per sicurezza
                    RefreshToken newRefreshToken = refreshTokenService.creaRefreshToken(user.getId());

                    log.info("Token rinnovati con successo per utente: {} (ID: {})", user.getUsername(), user.getId());

                    return RefreshTokenResponseDTO.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(newRefreshToken.getToken())
                            .type(AUTHORIZATION_TYPE)
                            .build();
                })
                .orElseThrow(() -> {
                    log.warn("Tentativo di refresh con token non valido o scaduto");
                    return new InvalidTokenException("Refresh token non valido o scaduto");
                });
    }

    /**
     * Effettua il logout invalidando tutti i refresh token dell'utente
     */
    @Transactional
    public void logout(Long userId) {
        log.info("Logout per utente ID: {}", userId);
        refreshTokenService.eliminaRefreshTokenUtente(userId);
        log.info("Logout completato per utente ID: {}", userId);
    }
}