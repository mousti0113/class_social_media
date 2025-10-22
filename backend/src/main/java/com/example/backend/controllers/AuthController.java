package com.example.backend.controllers;

import com.example.backend.dtos.request.LoginRequestDTO;
import com.example.backend.dtos.request.RegistrazioneRequestDTO;
import com.example.backend.dtos.request.RefreshTokenRequestDTO;
import com.example.backend.dtos.response.LoginResponseDTO;
import com.example.backend.dtos.response.RefreshTokenResponseDTO;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.models.User;
import com.example.backend.repositories.UserRepository;
import com.example.backend.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller per la gestione dell'autenticazione.
 * Tutti i metodi delegano la logica al AuthService e gestiscono solo HTTP.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    /**
     * POST /api/auth/registrazione
     * Registra un nuovo utente
     *
     * @param request DTO con username, email, password e nome completo
     * @return LoginResponseDTO con access token, refresh token e dati utente
     * @throws ResourceAlreadyExistsException se username o email già esistono
     * @throws LimitExceededException se si supera il limite di 17 studenti
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponseDTO> registrazione(@Valid @RequestBody RegistrazioneRequestDTO request) {
        log.debug("POST /api/auth/register - Username: {}", request.getUsername());

        LoginResponseDTO response = authService.registrazione(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * POST /api/auth/login
     * Effettua il login
     *
     * @param request DTO con username e password
     * @return LoginResponseDTO con access token, refresh token e dati utente
     * @throws InvalidCredentialsException se username o password non sono corretti
     * @throws ResourceNotFoundException se l'utente non esiste
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        log.debug("POST /api/auth/login - Username: {}", request.getUsername());

        LoginResponseDTO response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/refresh-token
     * Rinnova l'access token usando il refresh token
     *
     * @param request DTO con refresh token
     * @return RefreshTokenResponseDTO con nuovo access token e refresh token
     * @throws InvalidTokenException se il refresh token non è valido o è scaduto
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        log.debug("POST /api/auth/refresh-token");

        RefreshTokenResponseDTO response = authService.refreshToken(request);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/logout
     * Effettua il logout invalidando tutti i refresh token dell'utente
     *
     *  Richiede autenticazione (access token nell'header Authorization)
     *
     * @param userDetails Dettagli dell'utente autenticato (iniettato automaticamente)
     * @return Messaggio di conferma
     * @throws ResourceNotFoundException se l'utente non viene trovato
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal UserDetails userDetails) {
        log.debug("POST /api/auth/logout - Username: {}", userDetails.getUsername());

        // Recupera l'utente dal database
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Utente", "username", userDetails.getUsername()));

        // Effettua il logout
        authService.logout(user.getId());

        return ResponseEntity.ok("Logout effettuato con successo");
    }
}